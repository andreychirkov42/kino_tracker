package com.kursach.movietracker.integration.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class TmdbDtos {
    private TmdbDtos() {
    }

    public record Page<T>(int page, List<T> results, @JsonProperty("total_pages") int totalPages) {
    }

    public record GenreList(List<Genre> genres) {
    }

    public record Genre(int id, String name) {
    }

    public record MovieSummary(
        int id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        @JsonProperty("poster_path") String posterPath
    ) {
    }

    public record TvSummary(
        int id,
        String name,
        @JsonProperty("original_name") String originalName,
        @JsonProperty("first_air_date") String firstAirDate,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        @JsonProperty("poster_path") String posterPath
    ) {
    }

    public record MovieDetails(
        int id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("runtime") Integer runtime,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("homepage") String homepage,
        List<Genre> genres,
        Credits credits
    ) {
    }

    public record TvDetails(
        int id,
        String name,
        @JsonProperty("original_name") String originalName,
        @JsonProperty("first_air_date") String firstAirDate,
        @JsonProperty("number_of_seasons") Integer numberOfSeasons,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("homepage") String homepage,
        List<Genre> genres,
        @JsonProperty("created_by") List<Person> createdBy
    ) {
    }

    public record Credits(List<Person> crew) {
    }

    public record Person(int id, String name, String job) {
    }
}
