package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class SlashCommandListener {
    //An array list of classes that implement the SlashCommand interface
    private final static List<SlashCommand> commands = new ArrayList<>();

    static {
        //We register our commands here when the class is initialized
        commands.add(new TierListCommand());
        commands.add(new XmasCommand());
    }

    public static Mono<Message> handle(ChatInputInteractionEvent event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name of the command this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                // Get the first (and only) item in the flux that matches our filter
                .next()
                //have our command class handle all the logic related to its specific command.
                .flatMap(command -> command.handle(event));
    }
}
