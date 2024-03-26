package org.eu.polarexpress.conductor.discord.reaction;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.eu.polarexpress.conductor.util.OtherUtils.getFormDataAsString;

public class SauceListener {
    private static final String API_URL = "https://saucenao.com/search.php";
    private static final String FOOTER_URL =
            "https://cdn.discordapp.com/avatars/996034025842036816/8f53fdf39c01cbb3474ed0eb0cd094a2.webp?size=100";
    private static final String URL_REGEX = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\." +
            "[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";

    @ReactionListener(emoji = "\uD83C\uDF36\uFE0F")
    public static Mono<Void> fetchSauce(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .map(message -> fetchSauce(bot, message.getAttachments().getFirst()))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    private static EmbedCreateSpec fetchSauce(DiscordBot bot, Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        var imageUrl = attachment.getUrl();
        var form = new HashMap<String, String>();
        form.put("file", "(binary)");
        form.put("url", imageUrl);
        var resultPage = bot.getHttpHandler().postForm(API_URL,
                        getFormDataAsString(form),
                        "Content-Type", "application/x-www-form-urlencoded")
                .join();
        if (resultPage.statusCode() != 200) {
            bot.getLogger().error("Failed to fetch sauce!");
            return null;
        }
        Document doc = Jsoup.parse(resultPage.body());
        var resultTitle = doc.getElementsByClass("resulttitle").first();
        var resultPercentage = doc.getElementsByClass("resultsimilarityinfo").first();
        var resultImage = doc.getElementsByClass("resultimage").first();
        var image = Optional.ofNullable(resultImage).map(e -> e.getElementsByTag("img").first())
                .orElse(null);
        if (resultTitle == null || resultPercentage == null || image == null) {
            bot.getLogger().warn("No sauce result found!");
            return null;
        }
        var preview = image.attr("src");
        var previewFallback = image.attr("data-src");
        var sauces = doc.getElementsByClass("resultcontentcolumn").first();
        var fields = new ArrayList<EmbedCreateFields.Field>();
        if (sauces != null) {
            String subtitle = null;
            String source = null;
            String sourceLink = null;
            for (var child : sauces.children()) {
                if (subtitle != null && source != null && sourceLink != null) {
                    fields.add(EmbedCreateFields.Field.of(
                            subtitle,
                            String.format("[%s](%s '%s')", source, sourceLink, source),
                            false
                    ));
                    subtitle = null;
                    source = null;
                    sourceLink = null;
                }
                if (child.tag().getName().equals("strong")) {
                    subtitle = child.text();
                }
                if (child.tag().getName().equals("a")) {
                    source = child.text();
                    sourceLink = child.attr("href");
                }
            }
            if (subtitle != null && source != null && sourceLink != null) {
                fields.add(EmbedCreateFields.Field.of(
                        subtitle,
                        String.format("[%s](%s '%s')", source, sourceLink, source),
                        false
                ));
            }
        }
        return EmbedCreateSpec.builder()
                .color(Color.of(255, 67, 63))
                .title(String.format("%s - Similarity: %s", resultTitle.text(), resultPercentage.text()))
                .thumbnail(preview.matches(URL_REGEX) ? preview :
                        previewFallback.matches(URL_REGEX) ? previewFallback :
                                "https://cdn.discordapp.com/emojis/1189703319300608020.webp?size=48&quality=lossless")
                .addFields(fields.toArray(new EmbedCreateFields.Field[0]))
                .footer("Polar bear meat is delicious!", FOOTER_URL)
                .build();
    }

}
