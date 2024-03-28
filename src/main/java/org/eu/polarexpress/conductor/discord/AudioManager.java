package org.eu.polarexpress.conductor.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.voice.AudioProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class AudioManager {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer audioPlayer;
    private final AudioProvider provider;
    private final List<AudioReference> queue = new ArrayList<>();
    @Setter
    @Getter
    private boolean loop;

    public AudioManager() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        audioPlayer = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(audioPlayer);
        audioPlayer.addListener(event -> {
            if (event instanceof TrackEndEvent trackEndEvent) {
                if (trackEndEvent.endReason.mayStartNext) {
                    if (loop) {
                        audioPlayer.playTrack(trackEndEvent.track.makeClone());
                        return;
                    }
                    playNextTrack();
                }
            }
        });
    }

    public void addTrack(String url) {
        var audioRef = new AudioReference(url, null);
        if (audioPlayer.getPlayingTrack() == null) {
            TrackScheduler scheduler = new TrackScheduler(audioPlayer);
            playerManager.loadItem(audioRef, scheduler);
            return;
        }
        queue.add(audioRef);
    }

    public void stopCurrentTrack() {
        audioPlayer.startTrack(null, false);
    }

    public void playNextTrack() {
        if (!queue.isEmpty()) {
            TrackScheduler scheduler = new TrackScheduler(audioPlayer);
            var track = queue.getFirst();
            playerManager.loadItem(track, scheduler);
            queue.remove(track);
        }
    }

    public void removeTrack(int index) {
        queue.remove(index);
    }

    public void clearQueue() {
        queue.clear();
    }
}
