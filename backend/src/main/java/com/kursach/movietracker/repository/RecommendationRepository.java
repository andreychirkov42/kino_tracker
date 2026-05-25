package com.kursach.movietracker.repository;

import com.kursach.movietracker.model.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);

    void deleteByUserId(Long userId);

    void deleteByMediaContentId(Long mediaContentId);
}
