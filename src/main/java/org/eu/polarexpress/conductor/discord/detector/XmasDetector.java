package org.eu.polarexpress.conductor.discord.detector;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.eu.polarexpress.conductor.discord.DiscordBot;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class XmasDetector {
    private static final String REGEX_URL = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)";
    private static final String REGEX_ANY = ".+";

    @Detector(regex = REGEX_ANY)
    public static void checkXmas(DiscordBot bot, MessageCreateEvent event) {
        if (!"xmas".equalsIgnoreCase(bot.getMode())) {
            return;
        }
        var isNotBot = event.getMember().map(member -> !member.isBot()).orElse(true);
        var notEmpty = !event.getMessage().getContent().replaceAll(" ", "").isEmpty();
        var notLink = !event.getMessage().getContent().matches(REGEX_URL);
        var include = event.getMember()
                .map(member -> member.getRoles().any(role ->
                        role.getName().equalsIgnoreCase("xmasarrived")).block())
                .orElse(false);
        if (isNotBot && notEmpty && notLink && include) {
            var cleanContent = event.getMessage().getContent().replaceAll(" ", "").toLowerCase();
            var whitelist = List.of("hohoho", "merrychristmas", "padoru", "xmas", "jolly", "merry");
            var userId = event.getMember().map(member -> member.getId().asString()).orElse("");
            if (!userId.isEmpty() && whitelist.stream().noneMatch(cleanContent::contains)) {
                event.getMessage().delete().block();
                event.getMessage().getChannel()
                        .flatMap(channel ->
                                channel.createMessage("<@" +
                                        userId +
                                        "> You have been jollyfied!" +
                                        " Your messages have to contain any of the whitelisted words!"))
                        .delayElement(Duration.of(4, ChronoUnit.SECONDS))
                        .subscribe(message -> message.delete().block());
            }
        }
    }
}
