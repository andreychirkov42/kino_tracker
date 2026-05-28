package com.kursach.movietracker.service;

import com.kursach.movietracker.dto.WatchRecordRequest;
import com.kursach.movietracker.dto.WatchRecordResponse;
import com.kursach.movietracker.dto.WatchRecordUpdateRequest;
import com.kursach.movietracker.model.MediaContent;
import com.kursach.movietracker.model.UserEntity;
import com.kursach.movietracker.model.WatchRecord;
import com.kursach.movietracker.model.WatchStatus;
import com.kursach.movietracker.repository.WatchRecordRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WatchRecordService {
    private final WatchRecordRepository watchRecordRepository;
    private final UserService userService;
    private final CatalogService catalogService;
    private final RecommendationService recommendationService;

    public WatchRecordService(
        WatchRecordRepository watchRecordRepository,
        UserService userService,
        CatalogService catalogService,
        RecommendationService recommendationService
    ) {
        this.watchRecordRepository = watchRecordRepository;
        this.userService = userService;
        this.catalogService = catalogService;
        this.recommendationService = recommendationService;
    }

    @Transactional(readOnly = true)
    public List<WatchRecordResponse> getUserList(Long userId) {
        userService.getUser(userId);
        return watchRecordRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public WatchRecordResponse add(Long userId, WatchRecordRequest request) {
        UserEntity user = userService.getUser(userId);
        MediaContent content = catalogService.getEntity(request.mediaContentId());

        watchRecordRepository.findByUserIdAndMediaContentId(userId, request.mediaContentId())
            .ifPresent(record -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Фильм или сериал уже есть в личном списке");
            });

        WatchStatus status = request.status() == null ? WatchStatus.PLANNED : request.status();
        WatchRecord record = watchRecordRepository.save(new WatchRecord(user, content, status, request.rating()));
        recalculateAverageRating(content);
        recommendationService.invalidateForUser(userId);
        return toResponse(record);
    }

    @Transactional
    public WatchRecordResponse update(Long userId, Long recordId, WatchRecordUpdateRequest request) {
        WatchRecord record = watchRecordRepository.findById(recordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись просмотра не найдена"));
        if (!record.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нельзя изменить чужую запись просмотра");
        }

        if (request.status() != null) {
            record.changeStatus(request.status());
        }
        if (request.rating() != null) {
            record.setRating(request.rating());
        }

        WatchRecord saved = watchRecordRepository.save(record);
        recalculateAverageRating(saved.getMediaContent());
        recommendationService.invalidateForUser(userId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        WatchRecord record = watchRecordRepository.findById(recordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись просмотра не найдена"));
        if (!record.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нельзя удалить чужую запись просмотра");
        }
        MediaContent content = record.getMediaContent();
        watchRecordRepository.delete(record);
        watchRecordRepository.flush();
        recalculateAverageRating(content);
        recommendationService.invalidateForUser(userId);
    }

    private void recalculateAverageRating(MediaContent content) {
        content.calculateAverageRating(watchRecordRepository.findByMediaContentId(content.getId()));
    }

    private WatchRecordResponse toResponse(WatchRecord record) {
        return new WatchRecordResponse(
            record.getId(),
            record.getStatus(),
            record.getRating(),
            record.getAddedAt(),
            MediaContentMapper.toResponse(record.getMediaContent())
        );
    }
}
