package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface SlashCommand {
    String getName();

    Mono<Message> handle(ChatInputInteractionEvent event);
}
