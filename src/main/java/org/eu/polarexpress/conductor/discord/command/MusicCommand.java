package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelJoinSpec;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.discord.TrackScheduler;
import reactor.core.publisher.Mono;

import java.util.Arrays;

public class MusicCommand {

    @Command(command = "join")
    public static Mono<Void> join(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(channel -> channel.join(VoiceChannelJoinSpec.builder()
                        .provider(bot.getAudioManager().getProvider())
                        .build()))
                .then();
    }

    @Command(command = "play")
    public static Mono<Void> play(DiscordBot bot, MessageCreateEvent event) {
        TrackScheduler scheduler = new TrackScheduler(bot.getAudioManager().getAudioPlayer());
        return Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .filter(args -> args.size() > 1)
                .doOnNext(command ->
                        bot.getAudioManager().getPlayerManager().loadItem(command.get(1), scheduler))
                .then();
    }

    @Command(command = "volume")
    public static Mono<Void> volume(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .filter(args -> args.size() > 1 && args.get(1).matches("\\d+"))
                .doOnNext(command ->
                        bot.getAudioManager().getAudioPlayer().setVolume(Integer.parseInt(command.get(1))))
                .then();
    }

    @Command(command = "timeframe")
    public static Mono<Void> timeframe(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .filter(args -> args.size() > 1 && args.get(1).matches("\\d+"))
                .doOnNext(command ->
                        bot.getAudioManager().getAudioPlayer().getPlayingTrack().setPosition(Long.parseLong(command.get(1))))
                .then();
    }

    @Command(command = "pause")
    public static Mono<Void> pause(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .doOnNext(command -> bot.getAudioManager().getAudioPlayer().setPaused(true)).then();
    }

    @Command(command = "unpause")
    public static Mono<Void> unpause(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .doOnNext(command -> bot.getAudioManager().getAudioPlayer().setPaused(false)).then();
    }

    @Command(command = "quit")
    public static Mono<Void> quit(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMember())
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(VoiceChannel::sendDisconnectVoiceState)
                .then();
    }

}
