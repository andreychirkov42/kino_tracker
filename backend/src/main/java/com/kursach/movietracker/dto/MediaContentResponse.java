package com.kursach.movietracker.dto;

import com.kursach.movietracker.model.ContentType;
import java.util.List;

public record MediaContentResponse(
    Long id,
    String title,
    String originalTitle,
    ContentType contentType,
    Integer releaseYear,
    String duration,
    String description,
    String director,
    String mood,
    String posterUrl,
    String sourceUrl,
    Double averageRating,
    List<String> genres
) {
}
