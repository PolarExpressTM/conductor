package org.eu.polarexpress.conductor.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.voice.AudioProvider;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AudioManager {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer audioPlayer;
    private final AudioProvider provider;

    public AudioManager() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        audioPlayer = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(audioPlayer);
    }
}
