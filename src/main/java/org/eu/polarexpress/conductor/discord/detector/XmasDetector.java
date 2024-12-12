package org.eu.polarexpress.conductor.discord.detector;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class XmasDetector {
    private static final String REGEX_URL = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)";
    private static final String REGEX_ANY = ".+";
    @Value("${discord.mode}")
    private static String mode;

    @Detector(regex = REGEX_ANY)
    public static void checkXmas(DiscordBot bot, MessageCreateEvent event) {
        if (!mode.equalsIgnoreCase("xmas")) {
            return;
        }
        var debug = event.getMember().map(member ->
                member.getId().asString().equalsIgnoreCase("407936795490385922")).orElse(false);
        var isNotBot = event.getMember().map(member -> !member.isBot()).orElse(true);
        var notEmpty = !event.getMessage().getContent().replaceAll(" ", "").isEmpty();
        var notLink = !event.getMessage().getContent().matches(REGEX_URL);
        var exclude = event.getMember()
                .map(member -> member.getRoles().any(role ->
                        role.getName().equalsIgnoreCase("excludehohoho")).block())
                .orElse(false);
        if (debug && isNotBot &&  notEmpty && notLink && !exclude) {
            var cleanContent = event.getMessage().getContent().replaceAll(" ", "").toLowerCase();
            var whitelist = List.of("hohoho", "merrychristmas", "padoru", "xmas");
            var userId = event.getMember().map(member -> member.getId().asString());
            if (whitelist.stream().noneMatch(cleanContent::contains)) {
                event.getMessage().delete();
                event.getMessage().getChannel()
                        .flatMap(channel ->
                                channel.createMessage("<@" +
                                        userId +
                                        "> You have been jollyfied!" +
                                        " Your messages have to contain any of the whitelisted words!"))
                        .subscribe(message -> {
                            try {
                                CompletableFuture.supplyAsync(message::delete).get(4, TimeUnit.SECONDS);
                            } catch (TimeoutException e) {
                                bot.getLogger().error("Xmas timeout!");
                            } catch (InterruptedException | ExecutionException e) {
                                bot.getLogger().error("Xmas failed!");
                            }
                        });
            }
        }
    }
}
