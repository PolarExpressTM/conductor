package org.eu.polarexpress.conductor.dto;

import java.util.List;

public record UrbanResponse(
        int statusCode,
        String term,
        boolean found,
        int totalPages,
        List<UrbanData> data
) {
    public record UrbanData(
            String word,
            String meaning,
            String contributor,
            String date
    ) {
    }
}
