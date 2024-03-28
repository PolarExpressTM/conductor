package org.eu.polarexpress.conductor.dto;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Builder;

@Builder
public record TimeframeDto(
        long position,
        long duration,
        String title,
        String author,
        String artworkUrl
) {
    public static TimeframeDto fromAudioTrack(AudioTrack audioTrack) {
        return TimeframeDto.builder()
                .position(audioTrack.getPosition())
                .duration(audioTrack.getDuration())
                .title(audioTrack.getInfo().title)
                .author(audioTrack.getInfo().author)
                .artworkUrl(audioTrack.getInfo().artworkUrl)
                .build();
    }
}
