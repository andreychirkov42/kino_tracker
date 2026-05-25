package com.kursach.movietracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "media_content")
public class MediaContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "original_title", length = 180)
    private String originalTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType contentType;

    @Column(nullable = false)
    private Integer releaseYear;

    @Column(length = 60)
    private String duration;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String director;

    @Column(length = 80)
    private String mood;

    @Column(nullable = false, length = 600)
    private String posterUrl;

    @Column(length = 600)
    private String sourceUrl;

    @Column(nullable = false)
    private Double averageRating = 0.0;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "media_content_genres",
        joinColumns = @JoinColumn(name = "media_content_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new LinkedHashSet<>();

    @OneToMany(mappedBy = "mediaContent")
    private List<WatchRecord> watchRecords = new ArrayList<>();

    public MediaContent() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public String getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public String getDirector() {
        return director;
    }

    public String getMood() {
        return mood;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void updateInfo(
        String title,
        String originalTitle,
        ContentType contentType,
        Integer releaseYear,
        String duration,
        String description,
        String director,
        String mood,
        String posterUrl,
        String sourceUrl,
        Set<Genre> genres
    ) {
        this.title = title;
        this.originalTitle = originalTitle;
        this.contentType = contentType;
        this.releaseYear = releaseYear;
        this.duration = duration;
        this.description = description;
        this.director = director;
        this.mood = mood;
        this.posterUrl = posterUrl;
        this.sourceUrl = sourceUrl;
        this.genres = genres;
    }

    public void calculateAverageRating(List<WatchRecord> records) {
        this.averageRating = records.stream()
            .filter(record -> record.getRating() != null && record.getRating() > 0)
            .mapToInt(WatchRecord::getRating)
            .average()
            .orElse(0.0);
    }
}
