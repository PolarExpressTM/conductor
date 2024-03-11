package org.eu.polarexpress.conductor.dto;

import java.util.List;

public record IllustrationPagesResponse(
        boolean error,
        String message,
        List<IllustrationPages> body
) {
    public record IllustrationPages(
            IllustrationPagesUrls urls,
            int width,
            int height
    ) {
    }
    public record IllustrationPagesUrls(
            String thumb_mini,
            String small,
            String regular,
            String original
    ) {
        public List<String> getAllUrls() {
            return List.of(original, regular, small, thumb_mini);
        }
    }
}
