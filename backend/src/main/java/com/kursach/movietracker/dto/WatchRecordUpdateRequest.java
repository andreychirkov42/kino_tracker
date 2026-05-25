package com.kursach.movietracker.dto;

import com.kursach.movietracker.model.WatchStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record WatchRecordUpdateRequest(
    WatchStatus status,
    @Min(0) @Max(10) Integer rating
) {
}
