package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.rest.util.Color;
import org.eu.polarexpress.conductor.discord.DiscordBot;
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

    @Command(command = "info")
    public static Mono<Void> infoTrack(DiscordBot bot, MessageCreateEvent event) {
        return event.getMessage().getChannel()
                .flatMap(channel -> {
                    var currentTrack = bot.getAudioManager().getAudioPlayer().getPlayingTrack();
                    if (currentTrack != null) {
                        var embed = EmbedCreateSpec.builder()
                                .color(Color.of(255, 67, 63))
                                .title("Current track playing")
                                .thumbnail(currentTrack.getInfo().artworkUrl)
                                .addField("Source", String.format("[%s](%s '%s')",
                                        currentTrack.getInfo().title,
                                        currentTrack.getInfo().uri,
                                        currentTrack.getInfo().title), false)
                                .addField("Author", currentTrack.getInfo().author, false)
                                .addField("Time",
                                        currentTrack.getPosition() + "/" + currentTrack.getDuration(),
                                        false)
                                .build();
                        return channel.createMessage(embed);
                    } else {
                        return channel.createMessage("No track playing right now...");
                    }
                })
                .then();
    }

    @Command(command = "play")
    public static Mono<Void> play(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> Arrays.asList(content.split(" ")))
                .filter(args -> args.size() > 1)
                .doOnNext(command -> bot.getAudioManager().addTrack(command.get(1)))
                .then();
    }

    @Command(command = "stop")
    public static Mono<Void> stop(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .doOnNext(command -> bot.getAudioManager().stopCurrentTrack())
                .then();
    }

    @Command(command = "continue")
    public static Mono<Void> continueTrack(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .doOnNext(command -> bot.getAudioManager().playNextTrack())
                .then();
    }

    @Command(command = "skip")
    public static Mono<Void> skip(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .doOnNext(command -> bot.getAudioManager().playNextTrack())
                .then();
    }

    @Command(command = "loop")
    public static Mono<Void> loop(DiscordBot bot, MessageCreateEvent event) {
        return event.getMessage().getChannel()
                .flatMap(channel -> {
                    var loop = bot.getAudioManager().isLoop();
                    bot.getAudioManager().setLoop(!loop);
                    return channel.createMessage("Track is " + (!loop ? "" : "not") + " on loop now!");
                })
                .then();
    }

    @Command(command = "clear")
    public static Mono<Void> clear(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .doOnNext(command -> bot.getAudioManager().clearQueue())
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
