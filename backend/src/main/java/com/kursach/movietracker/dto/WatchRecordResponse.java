package com.kursach.movietracker.dto;

import com.kursach.movietracker.model.WatchStatus;
import java.time.LocalDateTime;

public record WatchRecordResponse(
    Long id,
    WatchStatus status,
    Integer rating,
    LocalDateTime addedAt,
    MediaContentResponse mediaContent
) {
}
