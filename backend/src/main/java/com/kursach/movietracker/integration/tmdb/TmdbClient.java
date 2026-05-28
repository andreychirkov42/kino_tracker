package com.kursach.movietracker.integration.tmdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.GenreList;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.MovieDetails;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.MovieSummary;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.Page;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.TvDetails;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.TvSummary;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TmdbClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String language;

    public TmdbClient(
        ObjectMapper objectMapper,
        @Value("${tmdb.api-key}") String apiKey,
        @Value("${tmdb.base-url}") String baseUrl,
        @Value("${tmdb.language}") String language,
        @Value("${tmdb.timeout-seconds}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.language = language;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Page<MovieSummary> popularMovies(int page) {
        return get(
            "/movie/popular?language=" + encode(language) + "&page=" + page,
            new TypeReference<>() {
            }
        );
    }

    public Page<TvSummary> popularTv(int page) {
        return get(
            "/tv/popular?language=" + encode(language) + "&page=" + page,
            new TypeReference<>() {
            }
        );
    }

    public MovieDetails movieDetails(int id) {
        return get(
            "/movie/" + id + "?language=" + encode(language) + "&append_to_response=credits",
            new TypeReference<>() {
            }
        );
    }

    public TvDetails tvDetails(int id) {
        return get(
            "/tv/" + id + "?language=" + encode(language),
            new TypeReference<>() {
            }
        );
    }

    public GenreList movieGenres() {
        return get("/genre/movie/list?language=" + encode(language), new TypeReference<>() {
        });
    }

    public GenreList tvGenres() {
        return get("/genre/tv/list?language=" + encode(language), new TypeReference<>() {
        });
    }

    private <T> T get(String pathWithQuery, TypeReference<T> type) {
        if (!isConfigured()) {
            throw new IllegalStateException("TMDB API key is not configured (tmdb.api-key)");
        }
        boolean bearer = apiKey.startsWith("eyJ");
        URI uri;
        if (bearer) {
            uri = URI.create(baseUrl + pathWithQuery);
        } else {
            String separator = pathWithQuery.contains("?") ? "&" : "?";
            uri = URI.create(baseUrl + pathWithQuery + separator + "api_key=" + apiKey);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json");
        if (bearer) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        HttpRequest request = builder.GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new TmdbException("TMDB returned HTTP " + response.statusCode() + " for " + pathWithQuery);
            }
            return objectMapper.readValue(response.body(), type);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new TmdbException("TMDB request failed: " + pathWithQuery, exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
