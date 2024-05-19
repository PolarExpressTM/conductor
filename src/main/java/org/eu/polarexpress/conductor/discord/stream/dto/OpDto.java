package org.eu.polarexpress.conductor.discord.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OpDto(
        @JsonProperty("op") int code,
        @JsonProperty("d") Object data
) {
}
