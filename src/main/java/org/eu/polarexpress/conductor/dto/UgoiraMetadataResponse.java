package org.eu.polarexpress.conductor.dto;

import java.util.List;

public record UgoiraMetadataResponse(
        boolean error,
        String message,
        UgoiraMetadata body
) {
    public record UgoiraMetadata(
            List<UgoiraFrame> frames,
            String mime_type,
            String originalSrc,
            String src
    ) {
    }
    public record UgoiraFrame(
            String file,
            int delay
    ) {
    }
}
