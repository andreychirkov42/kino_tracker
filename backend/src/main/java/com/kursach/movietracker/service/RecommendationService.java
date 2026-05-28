package com.kursach.movietracker.service;

import com.kursach.movietracker.dto.RecommendationResponse;
import com.kursach.movietracker.model.Genre;
import com.kursach.movietracker.model.MediaContent;
import com.kursach.movietracker.model.Recommendation;
import com.kursach.movietracker.model.UserEntity;
import com.kursach.movietracker.model.WatchRecord;
import com.kursach.movietracker.model.WatchStatus;
import com.kursach.movietracker.repository.MediaContentRepository;
import com.kursach.movietracker.repository.RecommendationRepository;
import com.kursach.movietracker.repository.WatchRecordRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private static final int RECOMMENDATION_LIMIT = 6;
    private static final int MIN_LIKED_RATING = 7;
    private static final double FAVORITE_BONUS = 3.0;
    private static final double SAME_DIRECTOR_BONUS = 6.0;

    private final RecommendationRepository recommendationRepository;
    private final WatchRecordRepository watchRecordRepository;
    private final MediaContentRepository mediaContentRepository;
    private final UserService userService;

    public RecommendationService(
        RecommendationRepository recommendationRepository,
        WatchRecordRepository watchRecordRepository,
        MediaContentRepository mediaContentRepository,
        UserService userService
    ) {
        this.recommendationRepository = recommendationRepository;
        this.watchRecordRepository = watchRecordRepository;
        this.mediaContentRepository = mediaContentRepository;
        this.userService = userService;
    }

    @Transactional
    public List<RecommendationResponse> getForUser(Long userId) {
        userService.getUser(userId);
        List<Recommendation> existing = recommendationRepository.findByUserIdOrderByScoreDesc(userId);
        if (existing.isEmpty()) {
            existing = rebuildForUser(userId);
        }
        return existing.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<RecommendationResponse> refreshForUser(Long userId) {
        return rebuildForUser(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void invalidateForUser(Long userId) {
        recommendationRepository.deleteByUserId(userId);
    }

    private List<Recommendation> rebuildForUser(Long userId) {
        UserEntity user = userService.getUser(userId);
        List<WatchRecord> records = watchRecordRepository.findByUserIdOrderByAddedAtDesc(userId);
        Set<Long> userContentIds = new HashSet<>();
        Map<String, Double> genreScores = new HashMap<>();

        for (WatchRecord record : records) {
            userContentIds.add(record.getMediaContent().getId());
            int rating = record.getRating() == null ? 0 : record.getRating();
            if (rating < MIN_LIKED_RATING) {
                continue;
            }
            double bonus = record.getStatus() == WatchStatus.FAVORITE ? FAVORITE_BONUS : 0.0;
            for (Genre genre : record.getMediaContent().getGenres()) {
                genreScores.merge(genre.getName(), rating + bonus, Double::sum);
            }
        }

        recommendationRepository.deleteByUserId(userId);
        recommendationRepository.flush();

        return mediaContentRepository.findAll().stream()
            .filter(content -> !userContentIds.contains(content.getId()))
            .map(content -> new Recommendation(
                user,
                content,
                calculateScore(content, records, genreScores),
                buildReason(content, records, genreScores)
            ))
            .sorted(Comparator.comparing(Recommendation::getScore).reversed())
            .limit(RECOMMENDATION_LIMIT)
            .map(recommendationRepository::save)
            .toList();
    }

    private double calculateScore(MediaContent content, List<WatchRecord> records, Map<String, Double> genreScores) {
        double score = content.getAverageRating();
        for (Genre genre : content.getGenres()) {
            score += genreScores.getOrDefault(genre.getName(), 0.0);
        }
        boolean sameDirector = content.getDirector() != null && records.stream()
            .anyMatch(record -> content.getDirector().equals(record.getMediaContent().getDirector()));
        if (sameDirector) {
            score += SAME_DIRECTOR_BONUS;
        }
        if (records.size() < 2) {
            score += content.getReleaseYear() / 100.0;
        }
        return score;
    }

    private String buildReason(MediaContent content, List<WatchRecord> records, Map<String, Double> genreScores) {
        if (records.size() < 2) {
            return "Мало данных, поэтому учитывается актуальность и средняя оценка контента.";
        }
        return content.getGenres().stream()
            .map(Genre::getName)
            .filter(genreScores::containsKey)
            .findFirst()
            .map(genre -> "Похоже на ваши высокие оценки в жанре «" + genre + "».")
            .orElse("Добавляет разнообразие к текущему личному списку.");
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        return new RecommendationResponse(
            recommendation.getId(),
            recommendation.getScore(),
            recommendation.getReason(),
            MediaContentMapper.toResponse(recommendation.getMediaContent())
        );
    }
}
