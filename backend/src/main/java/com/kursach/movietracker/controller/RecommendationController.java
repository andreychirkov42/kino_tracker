package com.kursach.movietracker.controller;

import com.kursach.movietracker.dto.RecommendationResponse;
import com.kursach.movietracker.service.RecommendationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RecommendationResponse> getForUser(@PathVariable Long userId) {
        return recommendationService.getForUser(userId);
    }

    @PostMapping("/refresh")
    public List<RecommendationResponse> refresh(@PathVariable Long userId) {
        return recommendationService.refreshForUser(userId);
    }
}
