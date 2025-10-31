package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class LandmineCommand implements SlashCommand {
    @Override
    public String getName() {
        return "landmine";
    }

    @Override
    public Mono<Message> handle(ChatInputInteractionEvent event) {
        var landmine = event.getOption("mode")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                .orElse(false);
        var roleId = event.getInteraction().getGuild().map(guild ->
                        guild.getRoles().filter(role -> role.getName().equalsIgnoreCase("landmine"))
                                .blockFirst())
                .block();
        if (roleId == null) {
            return event.deferReply().then(event.createFollowup().withContent("Role 'landmine' not found!"));
        }
        var guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            return event.deferReply().then(event.createFollowup().withContent("Error while fetching guild!"));
        }
        guild.getMembers()
                .filter(member -> !member.isBot())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(member -> {
                    if (landmine) {
                        member.addRole(roleId.getId()).block();
                    } else {
                        member.removeRole(roleId.getId()).block();
                    }
                    return Mono.empty();
                })
                .subscribe();
        return event.deferReply().then(event.createFollowup().withContent(
                (landmine ? "Added" : "Removed") + " landmine role " + (landmine ? "to" : "from") + " everyone!")
        );
    }
}
