package com.kursach.movietracker.controller;

import com.kursach.movietracker.dto.WatchRecordRequest;
import com.kursach.movietracker.dto.WatchRecordResponse;
import com.kursach.movietracker.dto.WatchRecordUpdateRequest;
import com.kursach.movietracker.service.WatchRecordService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/watchlist")
public class WatchRecordController {
    private final WatchRecordService watchRecordService;

    public WatchRecordController(WatchRecordService watchRecordService) {
        this.watchRecordService = watchRecordService;
    }

    @GetMapping
    public List<WatchRecordResponse> getUserList(@PathVariable Long userId) {
        return watchRecordService.getUserList(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchRecordResponse add(@PathVariable Long userId, @Valid @RequestBody WatchRecordRequest request) {
        return watchRecordService.add(userId, request);
    }

    @PatchMapping("/{recordId}")
    public WatchRecordResponse update(
        @PathVariable Long userId,
        @PathVariable Long recordId,
        @Valid @RequestBody WatchRecordUpdateRequest request
    ) {
        return watchRecordService.update(userId, recordId, request);
    }

    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId, @PathVariable Long recordId) {
        watchRecordService.delete(userId, recordId);
    }
}
