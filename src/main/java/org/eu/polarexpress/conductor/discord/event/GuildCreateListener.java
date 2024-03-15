package org.eu.polarexpress.conductor.discord.event;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import reactor.core.publisher.Mono;

public class GuildCreateListener {

    @Listener(event = GuildCreateEvent.class)
    public static Mono<Void> onGuildCreate(DiscordBot bot, Event event) {
        var ev = (GuildCreateEvent) event;

        return null;
    }

}
