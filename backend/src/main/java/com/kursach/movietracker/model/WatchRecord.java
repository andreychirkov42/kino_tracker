package com.kursach.movietracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "watch_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "media_content_id"})
)
public class WatchRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "media_content_id", nullable = false)
    private MediaContent mediaContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WatchStatus status;

    private Integer rating;

    @Column(nullable = false)
    private LocalDateTime addedAt;

    protected WatchRecord() {
    }

    public WatchRecord(UserEntity user, MediaContent mediaContent, WatchStatus status, Integer rating) {
        this.user = user;
        this.mediaContent = mediaContent;
        this.status = status;
        this.rating = rating;
        this.addedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public MediaContent getMediaContent() {
        return mediaContent;
    }

    public WatchStatus getStatus() {
        return status;
    }

    public Integer getRating() {
        return rating;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void changeStatus(WatchStatus status) {
        this.status = status;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
