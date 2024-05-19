package org.eu.polarexpress.conductor.discord.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionDto(
        @JsonProperty("op") int code,
        @JsonProperty("d") Session data
) {
    public record Session(
            @JsonProperty("secret_key") String secretKey
    ) {
    }
}
