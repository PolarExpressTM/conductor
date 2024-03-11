package org.eu.polarexpress.conductor.discord.pixiv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.MessageCreateSpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.dto.IllustrationDetailsResponse;
import org.eu.polarexpress.conductor.util.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class PixivHandler {
    private static final String BASE_URL = "https://www.pixiv.net";
    private static final String API_URL = "https://www.pixiv.net/ajax";

    private final Logger logger = LoggerFactory.getLogger(PixivHandler.class);
    private final HttpHandler httpHandler;
    private final ObjectMapper objectMapper;

    @Value("${pixiv.session}")
    private String sessionCookie;

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
            String response = httpHandler.get(BASE_URL)
                    .thenApply(HttpResponse::body)
                    .get();
            var loggedIn = response.contains("logout.php") ||
                    response.contains("pixiv.user.loggedIn = true") ||
                    response.contains("_gaq.push(['_setCustomVar', 1, 'login', 'yes'") ||
                    response.contains("var dataLayer = [{ login: 'yes',");
            logger.info("Pixiv login: {}", loggedIn);
            return loggedIn;
        } catch (ExecutionException | InterruptedException exception) {
            logger.error(exception.getMessage());
        }
        return false;
    }

    public IllustrationDetailsResponse getIllustration(String id) {
        if (!login()) {
            logger.warn("Pixiv not logged in!");
            return null;
        }
        try {
            String ref = BASE_URL + "/artworks/" + id;
            String response = httpHandler.get(API_URL + "/illust/" + id + "?lang=en&ref=" + ref)
                    .thenApply(HttpResponse::body)
                    .get();
            var illustrationDetails = objectMapper.readValue(response, IllustrationDetailsResponse.class);
            logger.info("{}", objectMapper.writeValueAsString(illustrationDetails));
            logger.info("{}", response);
            return illustrationDetails;
        } catch (ExecutionException | InterruptedException | JsonProcessingException exception) {
            logger.error(exception.getMessage());
        }
        return null;
    }

    public void uploadIllustration(DiscordBot bot, MessageCreateEvent event) {
        try {
            var input = event.getMessage().getContent().split("/artworks/");
            if (input.length < 2 || !input[1].matches("\\d+")) {
                return;
            }
            var illustrationDetails = getIllustration(input[1]);
            var imageStream = httpHandler.stream("https://i.pximg.net/c/250x250_80_a2/custom-thumb/img/2023/08/06/15/20/42/110578942_p0_custom1200.jpg")
                    .get()
                    .body();
            logger.info("{}", imageStream);
            /*File file = new File("C:\\Users\\Philip\\Downloads\\image1.png");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = imageStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException exception) {
                logger.error(exception.getMessage());
            }*/
            var message = MessageCreateSpec.builder()
                    .addFile("image1.png", imageStream)
                    .build();
            Mono.justOrEmpty(event.getMessage())
                    .map(msg -> msg.edit().withEmbeds())
                    .block();
            event.getMessage().getChannel()
                    .flatMap(channel -> channel.createMessage(message))
                    .block();
        } catch (ExecutionException | InterruptedException exception) {
            logger.error(exception.getMessage());
        }
    }

    public String getIllustrationPages(String id) {
        return API_URL + "/illust/" + id + "/pages";
    }

    public String getUgoiraMetadata(String id) {
        return API_URL + "/illust/" + id + "/ugoira_meta";
    }

}
