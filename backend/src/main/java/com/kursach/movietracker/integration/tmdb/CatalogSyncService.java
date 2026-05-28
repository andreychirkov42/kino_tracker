package com.kursach.movietracker.integration.tmdb;

import com.kursach.movietracker.dto.MediaContentRequest;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.MovieDetails;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.MovieSummary;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.Person;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.TvDetails;
import com.kursach.movietracker.integration.tmdb.TmdbDtos.TvSummary;
import com.kursach.movietracker.model.ContentType;
import com.kursach.movietracker.repository.MediaContentRepository;
import com.kursach.movietracker.service.CatalogService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogSyncService {
    private static final Logger log = LoggerFactory.getLogger(CatalogSyncService.class);

    private final TmdbClient tmdbClient;
    private final CatalogService catalogService;
    private final MediaContentRepository mediaContentRepository;
    private final String imageBaseUrl;
    private final int moviePages;
    private final int tvPages;

    public CatalogSyncService(
        TmdbClient tmdbClient,
        CatalogService catalogService,
        MediaContentRepository mediaContentRepository,
        @Value("${tmdb.image-base-url}") String imageBaseUrl,
        @Value("${tmdb.movie-pages}") int moviePages,
        @Value("${tmdb.tv-pages}") int tvPages
    ) {
        this.tmdbClient = tmdbClient;
        this.catalogService = catalogService;
        this.mediaContentRepository = mediaContentRepository;
        this.imageBaseUrl = imageBaseUrl;
        this.moviePages = moviePages;
        this.tvPages = tvPages;
    }

    public boolean isAvailable() {
        return tmdbClient.isConfigured();
    }

    @Transactional
    public SyncResult syncPopular() {
        if (!isAvailable()) {
            throw new TmdbException("TMDB API key is not configured");
        }
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (int page = 1; page <= moviePages; page++) {
            for (MovieSummary summary : tmdbClient.popularMovies(page).results()) {
                try {
                    MediaContentRequest request = toMovieRequest(tmdbClient.movieDetails(summary.id()));
                    if (request == null) {
                        skipped++;
                        continue;
                    }
                    if (mediaContentRepository.existsByTitleAndReleaseYear(request.title(), request.releaseYear())) {
                        skipped++;
                        continue;
                    }
                    catalogService.create(request);
                    created++;
                } catch (RuntimeException exception) {
                    log.warn("Skipped movie {} due to error: {}", summary.id(), exception.getMessage());
                    failed++;
                }
            }
        }

        for (int page = 1; page <= tvPages; page++) {
            for (TvSummary summary : tmdbClient.popularTv(page).results()) {
                try {
                    MediaContentRequest request = toTvRequest(tmdbClient.tvDetails(summary.id()));
                    if (request == null) {
                        skipped++;
                        continue;
                    }
                    if (mediaContentRepository.existsByTitleAndReleaseYear(request.title(), request.releaseYear())) {
                        skipped++;
                        continue;
                    }
                    catalogService.create(request);
                    created++;
                } catch (RuntimeException exception) {
                    log.warn("Skipped tv show {} due to error: {}", summary.id(), exception.getMessage());
                    failed++;
                }
            }
        }

        return new SyncResult(created, skipped, failed);
    }

    private MediaContentRequest toMovieRequest(MovieDetails details) {
        Integer year = parseYear(details.releaseDate());
        if (details.title() == null || details.title().isBlank() || year == null || year < 1900) {
            return null;
        }
        if (details.posterPath() == null || details.posterPath().isBlank()) {
            return null;
        }
        String director = details.credits() == null || details.credits().crew() == null
            ? null
            : details.credits().crew().stream()
                .filter(person -> "Director".equals(person.job()))
                .map(Person::name)
                .findFirst()
                .orElse(null);
        return new MediaContentRequest(
            details.title(),
            details.originalTitle(),
            ContentType.MOVIE,
            year,
            formatRuntime(details.runtime()),
            blankToFallback(details.overview(), "Описание отсутствует."),
            director,
            null,
            imageBaseUrl + details.posterPath(),
            details.homepage(),
            extractGenreNames(details.genres())
        );
    }

    private MediaContentRequest toTvRequest(TvDetails details) {
        Integer year = parseYear(details.firstAirDate());
        if (details.name() == null || details.name().isBlank() || year == null || year < 1900) {
            return null;
        }
        if (details.posterPath() == null || details.posterPath().isBlank()) {
            return null;
        }
        String creator = details.createdBy() == null || details.createdBy().isEmpty()
            ? null
            : details.createdBy().get(0).name();
        return new MediaContentRequest(
            details.name(),
            details.originalName(),
            ContentType.SERIES,
            year,
            formatSeasons(details.numberOfSeasons()),
            blankToFallback(details.overview(), "Описание отсутствует."),
            creator,
            null,
            imageBaseUrl + details.posterPath(),
            details.homepage(),
            extractGenreNames(details.genres())
        );
    }

    private List<String> extractGenreNames(List<TmdbDtos.Genre> genres) {
        List<String> names = new ArrayList<>();
        if (genres == null) {
            return names;
        }
        for (TmdbDtos.Genre genre : genres) {
            if (genre.name() != null && !genre.name().isBlank()) {
                names.add(genre.name());
            }
        }
        if (names.isEmpty()) {
            names.add("Без жанра");
        }
        return names;
    }

    private Integer parseYear(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String formatRuntime(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours == 0) {
            return rest + " мин";
        }
        if (rest == 0) {
            return hours + " ч";
        }
        return hours + " ч " + rest + " мин";
    }

    private String formatSeasons(Integer seasons) {
        if (seasons == null || seasons <= 0) {
            return null;
        }
        return seasons + (seasons == 1 ? " сезон" : seasons < 5 ? " сезона" : " сезонов");
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record SyncResult(int created, int skipped, int failed) {
    }
}
