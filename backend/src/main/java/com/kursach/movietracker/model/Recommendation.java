package com.kursach.movietracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "media_content_id", nullable = false)
    private MediaContent mediaContent;

    @Column(nullable = false)
    private Double score;

    @Column(length = 240)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Recommendation() {
    }

    public Recommendation(UserEntity user, MediaContent mediaContent, Double score, String reason) {
        this.user = user;
        this.mediaContent = mediaContent;
        this.score = score;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MediaContent getMediaContent() {
        return mediaContent;
    }

    public Double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}
