package org.eu.polarexpress.conductor.discord.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReadyDto(
        @JsonProperty("op") int code,
        @JsonProperty("d") Ready data
) {
    public record Ready(
            int ssrc,
            String ip,
            int port,
            List<String> modes
    ) {
    }
}
