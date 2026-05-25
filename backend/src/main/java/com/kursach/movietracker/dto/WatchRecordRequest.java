package com.kursach.movietracker.dto;

import com.kursach.movietracker.model.WatchStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WatchRecordRequest(
    @NotNull Long mediaContentId,
    @NotNull WatchStatus status,
    @Min(0) @Max(10) Integer rating
) {
}
