package org.eu.polarexpress.conductor.dto;

public record IllustrationDetailsResponse(
        boolean error,
        String message,
        IllustrationDetails body
) {
    public record IllustrationDetails(
            String id,
            String title,
            String description,
            IllustrationType illustType,
            IllustrationDetailsUrls urls,
            int pageCount
    ) {

    }
    public record IllustrationDetailsUrls(
            String mini,
            String thumb,
            String small,
            String regular,
            String original
    ) {
    }

    public enum IllustrationType {
        ILLUSTRATION,
        UNKNOWN,
        UGOIRA;
    }
}
