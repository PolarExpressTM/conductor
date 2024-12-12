package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import reactor.core.publisher.Mono;

public class PingCommand {

    @Command(command = "ping")
    public static Mono<Void> ping(DiscordBot bot, MessageCreateEvent event) {
        return event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("Pong!"))
                .then();
    }

}
