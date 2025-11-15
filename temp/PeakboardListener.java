package org.eu.polarexpress.conductor.discord.reaction;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeakboardListener {

    @ReactionListener(emoji = "1410372274695180432")
    public static Mono<Void> peakBoard(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull(message -> processPeak(message, ev.getUserId().asString()))
                .then();
    }

    private static EmbedCreateSpec processPeak(Message message, String userId) {
        var originalGuild = message.getGuild().blockOptional();
        var originalChannel = message.getChannel().blockOptional();
        var temp = message.getReactions()
                .stream()
                .filter(reaction -> reaction.getEmoji().asCustomEmoji().isPresent() &&
                        "1410372274695180432".equals(reaction.getEmoji().asCustomEmoji().get().getId().asString()))
                .findFirst();
        if (temp.isEmpty()) {
            return null;
        }
        var reactedStars = message.getReactors(temp.get().getEmoji()).count().blockOptional().orElse(0L);
        if (userId.equals(message.getAuthor().get().getId().asString())) {
            reactedStars--;
        }
        if (reactedStars < 3) {
            return null;
        }
        var messageChannel = message.getGuild()
                .flatMap(guild -> guild.getChannelById(Snowflake.of("992158735034236989"))
                        .map(channel -> (MessageChannel) channel))
                .blockOptional();
        if (messageChannel.isPresent()) {
            var messages = messageChannel.get().getMessagesBefore(Snowflake.of(Instant.now())).collectList().blockOptional();
            if (messages.isPresent()) {
                var starred = messages.get().stream()
                        .filter(msg -> {
                            var content = msg.getContent();
                            return content.startsWith("<:peakfiction:1410372274695180432>") && content.endsWith(message.getId().asString());
                        })
                        .findFirst();
                if (starred.isPresent()) {
                    var embed = starred.get().getEmbeds().getFirst();
                    var stars = findStars(starred.get().getContent());
                    var image = message.getAttachments().isEmpty() ? "" : message.getAttachments().getFirst().getUrl();
                    starred.get().edit()
                            .withContentOrNull(String.format(
                                    "%s **%d** | %s",
                                    "<:peakfiction:1410372274695180432>",
                                    Math.min(reactedStars, stars + 1),
                                    String.format(
                                            "https://discord.com/channels/%s/%s/%s",
                                            originalGuild.get().getId().asString(),
                                            originalChannel.get().getId().asString(),
                                            message.getId().asString()
                                    )
                            ))
                            .withEmbeds(EmbedCreateSpec.builder()
                                    .color(embed.getColor().orElse(Color.of(255, 67, 63)))
                                    .description(embed.getDescription().orElse(""))
                                    .author(message.getAuthor().get().getTag(), null, message.getAuthor().get().getAvatarUrl())
                                    .timestamp(embed.getTimestamp().orElse(Instant.now()))
                                    .image(image)
                                    .build())
                            .block();
                    return null;
                }
            }
            var image = message.getAttachments().isEmpty() ? "" : message.getAttachments().getFirst().getUrl();
            messageChannel.get().createMessage(MessageCreateSpec.builder().build()
                            .withContent(String.format(
                                    "%s **%d** | %s",
                                    "<:peakfiction:1410372274695180432>",
                                    reactedStars,
                                    String.format(
                                            "https://discord.com/channels/%s/%s/%s",
                                            originalGuild.get().getId().asString(),
                                            originalChannel.get().getId().asString(),
                                            message.getId().asString()
                                    )
                            ))
                            .withEmbeds(EmbedCreateSpec.builder()
                                    .color(Color.of(255, 67, 63))
                                    .description(message.getContent())
                                    .author(message.getAuthor().get().getTag(), null, message.getAuthor().get().getAvatarUrl())
                                    .timestamp(Instant.now())
                                    .image(image)
                                    .build()))
                    .block();
        }
        return null;
    }

    private static int findStars(String stringToSearch) {
        Pattern integerPattern = Pattern.compile(".+\\s\\*\\*([0-9]{1,3})\\*\\*\\s\\|\\s(.+)");
        Matcher matcher = integerPattern.matcher(stringToSearch);
        int result = -1;
        while (matcher.find()) {
            try {
                result = Integer.parseInt(matcher.group(1));
                break;
            } catch (Exception ignored) {
            }
        }
        return result;
    }

}
