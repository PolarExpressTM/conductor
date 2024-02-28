package org.eu.polarexpress.conductor.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.VoiceChannelJoinSpec;
import lombok.Getter;
import org.eu.polarexpress.conductor.discord.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class DiscordBot {
    @Value("${discord.token}")
    private String token;
    private final Map<String, Command> commands;
    @Getter
    private final AudioManager audioManager;
    @Getter
    private GatewayDiscordClient client;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

    @Autowired
    public DiscordBot(AudioManager audioManager) {
        commands = new HashMap<>();
        this.audioManager = audioManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startDiscordBot() {
        logger.info("Connecting bot...");
        initCommands();
        connect();
    }

    public void connect() {
        client = DiscordClientBuilder.create(token).build()
                .login()
                .block();
        if (client != null) {
            client.getEventDispatcher().on(MessageCreateEvent.class)
                    .flatMap(event -> Mono.just(event.getMessage().getContent())
                            .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                    // We will be using ! as our "prefix" to any command in the system.
                                    .filter(entry -> content.startsWith("=!" + entry.getKey()))
                                    .flatMap(entry -> entry.getValue().execute(event))
                                    .next()))
                    .subscribe();
            client.onDisconnect().block();
        }
    }

    private void initCommands() {
        commands.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("Pong!"))
                .then());
        commands.put("join", event -> Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                // join returns a VoiceConnection which would be required if we were
                // adding disconnection features, but for now we are just ignoring it.
                .flatMap(channel -> channel.join(VoiceChannelJoinSpec.builder()
                        .provider(audioManager.getProvider())
                        .build()))
                .then());
        TrackScheduler scheduler = new TrackScheduler(audioManager.getAudioPlayer());
        commands.put("play", event -> Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .doOnNext(command -> audioManager.getPlayerManager().loadItem(command.get(1), scheduler))
                .then());
    }

    public Map<String, Command> getCommands() {
        return Collections.unmodifiableMap(commands);
    }
}
