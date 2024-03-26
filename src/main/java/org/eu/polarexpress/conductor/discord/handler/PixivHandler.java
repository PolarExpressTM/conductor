package org.eu.polarexpress.conductor.discord.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.dto.IllustrationDetailsResponse;
import org.eu.polarexpress.conductor.dto.IllustrationPagesResponse;
import org.eu.polarexpress.conductor.dto.UgoiraMetadataResponse;
import org.eu.polarexpress.conductor.util.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class PixivHandler {
    private static final long MAX_SIZE = 24_117_248;
    private static final String BASE_URL = "https://www.pixiv.net";
    private static final String API_URL = "https://www.pixiv.net/ajax";

    private final Logger logger = LoggerFactory.getLogger(PixivHandler.class);
    private final HttpHandler httpHandler;
    private final ObjectMapper objectMapper;

    @Value("${pixiv.session}")
    private String sessionCookie;
    @Value("${pixiv.max-pages}")
    private int maxPages;

    public void initCookie() {
        httpHandler.addCookie(
                "PHPSESSID",
                sessionCookie,
                "/",
                "pixiv.net",
                0,
                BASE_URL
        );
    }

    public boolean login() {
        try {
            var response = httpHandler.get(BASE_URL)
                    .get();
            var content = response.body();
            var loggedIn = content.contains("logout.php") ||
                    content.contains("pixiv.user.loggedIn = true") ||
                    content.contains("_gaq.push(['_setCustomVar', 1, 'login', 'yes'") ||
                    content.contains("var dataLayer = [{ login: 'yes',");
            logger.info("Pixiv login: {}", response);//https://www.pixiv.net/en/artworks/116830257
            logger.info("Pixiv login: {}", loggedIn);
            return loggedIn;
        } catch (ExecutionException | InterruptedException exception) {
            logger.error(exception.getMessage());
        }
        return false;
    }

    public void uploadIllustration(DiscordBot bot, MessageCreateEvent event) {
        try {
            var input = event.getMessage().getContent().split("/artworks/");
            if (input.length < 2 || !input[1].matches("\\d+")) {
                return;
            }
            var illustrationDetails = getIllustration(input[1]);
            if (illustrationDetails.body().illustType() == IllustrationDetailsResponse.IllustrationType.UGOIRA) {
                return;
            }
            if (illustrationDetails.body().pageCount() == 1) {
                processSingleIllustration(event, illustrationDetails);
            } else {
                processMultipleIllustrations(event, illustrationDetails);
            }
        } catch (ExecutionException | InterruptedException exception) {
            logger.error("uploadIllustration: {}", exception.getMessage());
        }
    }

    public IllustrationDetailsResponse getIllustration(String id) {
        try {
            String ref = BASE_URL + "/artworks/" + id;
            String response = httpHandler.getJson(API_URL + "/illust/" + id +
                            "?lang=en&ref=" + ref)
                    .thenApply(HttpResponse::body)
                    .get();
            var illustrationDetails = objectMapper.readValue(response, IllustrationDetailsResponse.class);
            logger.info("getIllustration: {}", objectMapper.writeValueAsString(illustrationDetails));
            return illustrationDetails;
        } catch (ExecutionException | InterruptedException | JsonProcessingException exception) {
            logger.error("getIllustration: {}", exception.getMessage());
        }
        return null;
    }

    public IllustrationPagesResponse getIllustrationPages(String id) {
        try {
            String ref = BASE_URL + "/artworks/" + id;
            String response = httpHandler.getJson(API_URL + "/illust/" + id + "/pages" +
                            "?lang=en&ref=" + ref)
                    .thenApply(HttpResponse::body)
                    .get();
            var illustrationDetails = objectMapper.readValue(response, IllustrationPagesResponse.class);
            logger.info("getIllustrationPages: {}", objectMapper.writeValueAsString(illustrationDetails));
            return illustrationDetails;
        } catch (ExecutionException | InterruptedException | JsonProcessingException exception) {
            logger.error("getIllustrationPages: {}", exception.getMessage());
        }
        return null;
    }

    public UgoiraMetadataResponse getUgoiraMetadata(String id) {
        try {
            String ref = BASE_URL + "/artworks/" + id;
            String response = httpHandler.getJson(API_URL + "/illust/" + id + "/ugoira_meta" +
                            "?lang=en&ref=" + ref)
                    .thenApply(HttpResponse::body)
                    .get();
            var illustrationDetails = objectMapper.readValue(response, UgoiraMetadataResponse.class);
            logger.info("getUgoiraMetadata: {}", objectMapper.writeValueAsString(illustrationDetails));
            return illustrationDetails;
        } catch (ExecutionException | InterruptedException | JsonProcessingException exception) {
            logger.error("getUgoiraMetadata: {}", exception.getMessage());
        }
        return null;
    }

    private void processSingleIllustration(MessageCreateEvent event, IllustrationDetailsResponse illustrationDetails)
            throws ExecutionException, InterruptedException {
        var highestImage = getHighestQualityImage(illustrationDetails.body().urls().getAllUrls());
        if (highestImage.isEmpty()) {
            logger.warn("No image found!");
            return;
        }
        var imageStream = httpHandler.stream(highestImage.get())
                .get()
                .body();
        var fileName = highestImage.get().split("/");
        var message = MessageCreateSpec.builder()
                .addFile(fileName[fileName.length - 1], imageStream)
                .build();
        Mono.justOrEmpty(event.getMessage())
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull(msg -> msg.edit().withFlags(Message.Flag.SUPPRESS_EMBEDS).block())
                .block();
        event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(message))
                .block();
    }

    private void processMultipleIllustrations(MessageCreateEvent event, IllustrationDetailsResponse illustrationDetails)
            throws ExecutionException, InterruptedException {
        var illustrationPages = getIllustrationPages(illustrationDetails.body().id());
        var pages = illustrationPages.body().subList(0, Math.min(illustrationPages.body().size(), maxPages));
        MessageCreateFields.File[] files = new MessageCreateFields.File[illustrationDetails.body().pageCount()];
        int i = 0;
        for (var page : pages) {
            var highestImage = getHighestQualityImage(page.urls().getAllUrls());
            if (highestImage.isEmpty()) {
                logger.warn("No image found!");
                return;
            }
            var imageStream = httpHandler.stream(highestImage.get())
                    .get()
                    .body();
            var fileName = highestImage.get().split("/");
            files[i++] = MessageCreateFields.File.of(fileName[fileName.length - 1], imageStream);
        }
        var message = MessageCreateSpec.builder()
                .addFiles(files)
                .build();
        Mono.justOrEmpty(event.getMessage())
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull(msg -> msg.edit().withFlags(Message.Flag.SUPPRESS_EMBEDS).block())
                .block();
        event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(message))
                .block();
    }

    private Optional<String> getHighestQualityImage(List<String> urls) {
        try {
            for (String url : urls) {
                if (url == null) {
                    continue;
                }
                var response = httpHandler.head(url).get();
                logger.info("getHighestQualityImage: {}", response);
                var contentLength = response.headers().firstValueAsLong("Content-Length");
                if (contentLength.isPresent() && contentLength.getAsLong() < MAX_SIZE) {
                    return Optional.of(url);
                }
            }
        } catch (InterruptedException | ExecutionException exception) {
            logger.error(exception.getMessage());
        }
        return Optional.empty();
    }

}
