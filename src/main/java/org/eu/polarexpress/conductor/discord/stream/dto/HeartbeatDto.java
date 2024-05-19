package org.eu.polarexpress.conductor.discord.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeartbeatDto(
        @JsonProperty("op") int code,
        @JsonProperty("d") Heartbeat data
) {
    public record Heartbeat(
            @JsonProperty("heartbeat_interval") int interval
    ) {
    }
}
