package org.eu.polarexpress.conductor.discord.detector;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.eu.polarexpress.conductor.discord.DiscordBot;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.SplittableRandom;

public class XmasDetector {
    private static final String REGEX_URL = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)";
    private static final String REGEX_ANY = ".+";

    @Detector(regex = REGEX_ANY)
    public static void checkXmas(DiscordBot bot, MessageCreateEvent event) {
        try {
            switch (bot.getMode()) {
                case "xmas":
                    processXmas(bot, event);
                    break;
                case "landmine":
                    processLandmine(bot, event);
                    break;
                default:
            }
        } catch (Exception e) {
            bot.getLogger().error(e.getMessage());
        }
    }

    private static void processXmas(DiscordBot bot, MessageCreateEvent event) {
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

    private static void processLandmine(DiscordBot bot, MessageCreateEvent event) {
        var isNotBot = event.getMember().map(member -> !member.isBot()).orElse(true);
        var notEmpty = !event.getMessage().getContent().replaceAll(" ", "").isEmpty();
        var notLink = !event.getMessage().getContent().matches(REGEX_URL);
        var include = event.getMember()
                .map(member -> member.getRoles().any(role ->
                        role.getName().equalsIgnoreCase("landmine")).block())
                .orElse(false);
        if (isNotBot && notEmpty && notLink && include) {
            var cleanContent = event.getMessage().getContent().replaceAll(" ", "").toLowerCase();
            var whitelist = List.of("wait", "bruh", "french", "naruhodo");
            var userId = event.getMember().map(member -> member.getId().asString()).orElse("");
            if (bot.getMutedUsers().containsKey(userId)) {
                if (System.currentTimeMillis() - bot.getMutedUsers().get(userId) > 60000) {
                    bot.getMutedUsers().remove(userId);
                    return;
                }
                event.getMessage().delete().block();
                return;
            }
            if (!userId.isEmpty() && !bot.getMutedUsers().containsKey(userId)) {
                var hasWhitelist = whitelist.stream().anyMatch(cleanContent::contains);
                var random = new SplittableRandom();
                var rng = random.nextInt(9000);
                System.out.println("RNG: " + rng);
                if (rng < 90 || (hasWhitelist && rng < 90 * 5)) {
                    bot.getMutedUsers().put(userId, System.currentTimeMillis());
                    event.getMessage().delete().block();
                    event.getMessage().getChannel()
                            .flatMap(channel ->
                                    channel.createMessage("\uD83D\uDCA5 <@" +
                                            userId +
                                            "> stepped on a landmine and has been timed out for 1 minute!"))
                            .delayElement(Duration.of(4, ChronoUnit.SECONDS))
                            .subscribe(message -> message.delete().block());
                }
            }
        }
    }
}
