package com.kursach.movietracker.service;

import com.kursach.movietracker.dto.MediaContentResponse;
import com.kursach.movietracker.model.Genre;
import com.kursach.movietracker.model.MediaContent;
import java.util.Comparator;

public final class MediaContentMapper {
    private MediaContentMapper() {
    }

    public static MediaContentResponse toResponse(MediaContent content) {
        return new MediaContentResponse(
            content.getId(),
            content.getTitle(),
            content.getOriginalTitle(),
            content.getContentType(),
            content.getReleaseYear(),
            content.getDuration(),
            content.getDescription(),
            content.getDirector(),
            content.getMood(),
            content.getPosterUrl(),
            content.getSourceUrl(),
            content.getAverageRating(),
            content.getGenres().stream()
                .map(Genre::getName)
                .sorted(Comparator.naturalOrder())
                .toList()
        );
    }
}
