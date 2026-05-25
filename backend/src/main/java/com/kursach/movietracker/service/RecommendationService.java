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
    public List<RecommendationResponse> buildForUser(Long userId) {
        UserEntity user = userService.getUser(userId);
        List<WatchRecord> records = watchRecordRepository.findByUserIdOrderByAddedAtDesc(userId);
        Set<Long> userContentIds = new HashSet<>();
        Map<String, Double> genreScores = new HashMap<>();

        for (WatchRecord record : records) {
            userContentIds.add(record.getMediaContent().getId());
            int rating = record.getRating() == null ? 0 : record.getRating();
            if (rating < 7) {
                continue;
            }
            double favoriteBonus = record.getStatus() == WatchStatus.FAVORITE ? 3.0 : 0.0;
            for (Genre genre : record.getMediaContent().getGenres()) {
                genreScores.merge(genre.getName(), rating + favoriteBonus, Double::sum);
            }
        }

        recommendationRepository.deleteByUserId(userId);
        List<Recommendation> saved = mediaContentRepository.findAll().stream()
            .filter(content -> !userContentIds.contains(content.getId()))
            .map(content -> new Recommendation(user, content, calculateScore(content, records, genreScores)))
            .sorted(Comparator.comparing(Recommendation::getScore).reversed())
            .limit(6)
            .map(recommendationRepository::save)
            .toList();

        return saved.stream()
            .map(recommendation -> new RecommendationResponse(
                recommendation.getId(),
                recommendation.getScore(),
                buildReason(recommendation.getMediaContent(), records, genreScores),
                MediaContentMapper.toResponse(recommendation.getMediaContent())
            ))
            .toList();
    }

    private double calculateScore(MediaContent content, List<WatchRecord> records, Map<String, Double> genreScores) {
        double score = content.getAverageRating();
        for (Genre genre : content.getGenres()) {
            score += genreScores.getOrDefault(genre.getName(), 0.0);
        }
        boolean sameDirector = records.stream()
            .anyMatch(record -> content.getDirector() != null && content.getDirector().equals(record.getMediaContent().getDirector()));
        if (sameDirector) {
            score += 6.0;
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
}
