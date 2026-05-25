package com.kursach.movietracker.dto;

public record RecommendationResponse(
    Long id,
    Double score,
    String reason,
    MediaContentResponse mediaContent
) {
}
