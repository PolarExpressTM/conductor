package org.eu.polarexpress.conductor.discord.detector;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.eu.polarexpress.conductor.discord.DiscordBot;

public class PixivDetector {
    private static final String REGEX_PIXIV = "https?://(www\\.)?pixiv\\.net/.*artworks/(?<id>\\d+)/?";

    @Detector(regex = REGEX_PIXIV)
    public static void getIllustration(DiscordBot bot, MessageCreateEvent event) {
        bot.getPixivHandler().uploadIllustration(bot, event);
    }

}
