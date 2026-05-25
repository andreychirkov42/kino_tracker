package com.kursach.movietracker.dto;

import com.kursach.movietracker.model.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MediaContentRequest(
    @NotBlank String title,
    String originalTitle,
    @NotNull ContentType contentType,
    @NotNull @Min(1900) Integer releaseYear,
    String duration,
    @NotBlank String description,
    String director,
    String mood,
    @NotBlank String posterUrl,
    String sourceUrl,
    @NotEmpty List<String> genres
) {
}
