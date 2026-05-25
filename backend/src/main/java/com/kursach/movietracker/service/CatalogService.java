package com.kursach.movietracker.service;

import com.kursach.movietracker.dto.MediaContentRequest;
import com.kursach.movietracker.dto.MediaContentResponse;
import com.kursach.movietracker.model.ContentType;
import com.kursach.movietracker.model.Genre;
import com.kursach.movietracker.model.MediaContent;
import com.kursach.movietracker.repository.GenreRepository;
import com.kursach.movietracker.repository.MediaContentRepository;
import com.kursach.movietracker.repository.RecommendationRepository;
import com.kursach.movietracker.repository.WatchRecordRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {
    private final MediaContentRepository mediaContentRepository;
    private final GenreRepository genreRepository;
    private final WatchRecordRepository watchRecordRepository;
    private final RecommendationRepository recommendationRepository;

    public CatalogService(
        MediaContentRepository mediaContentRepository,
        GenreRepository genreRepository,
        WatchRecordRepository watchRecordRepository,
        RecommendationRepository recommendationRepository
    ) {
        this.mediaContentRepository = mediaContentRepository;
        this.genreRepository = genreRepository;
        this.watchRecordRepository = watchRecordRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional(readOnly = true)
    public List<MediaContentResponse> search(String query, ContentType type, String genre, Integer year, Double minRating) {
        String normalizedQuery = blankToNull(query);
        String normalizedGenre = blankToNull(genre);
        return mediaContentRepository.search(normalizedQuery, type, normalizedGenre, year, minRating)
            .stream()
            .map(MediaContentMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public MediaContent getEntity(Long id) {
        return mediaContentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм или сериал не найден"));
    }

    @Transactional(readOnly = true)
    public MediaContentResponse getById(Long id) {
        return MediaContentMapper.toResponse(getEntity(id));
    }

    @Transactional
    public MediaContentResponse create(MediaContentRequest request) {
        MediaContent content = new MediaContent();
        applyRequest(content, request);
        return MediaContentMapper.toResponse(mediaContentRepository.save(content));
    }

    @Transactional
    public MediaContentResponse update(Long id, MediaContentRequest request) {
        MediaContent content = getEntity(id);
        applyRequest(content, request);
        return MediaContentMapper.toResponse(mediaContentRepository.save(content));
    }

    @Transactional
    public void delete(Long id) {
        if (!mediaContentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм или сериал не найден");
        }
        recommendationRepository.deleteByMediaContentId(id);
        watchRecordRepository.deleteByMediaContentId(id);
        mediaContentRepository.deleteById(id);
    }

    private void applyRequest(MediaContent content, MediaContentRequest request) {
        content.updateInfo(
            request.title().trim(),
            request.originalTitle() == null || request.originalTitle().isBlank()
                ? request.title().trim()
                : request.originalTitle().trim(),
            request.contentType(),
            request.releaseYear(),
            request.duration(),
            request.description().trim(),
            request.director(),
            request.mood(),
            request.posterUrl().trim(),
            request.sourceUrl(),
            resolveGenres(request.genres())
        );
    }

    private Set<Genre> resolveGenres(List<String> names) {
        Set<Genre> genres = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String normalized = name.trim();
            Genre genre = genreRepository.findByName(normalized)
                .orElseGet(() -> genreRepository.save(new Genre(normalized)));
            genres.add(genre);
        }
        if (genres.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите хотя бы один жанр");
        }
        return genres;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
