package com.kursach.movietracker.service;

import com.kursach.movietracker.dto.WatchRecordRequest;
import com.kursach.movietracker.dto.WatchRecordResponse;
import com.kursach.movietracker.dto.WatchRecordUpdateRequest;
import com.kursach.movietracker.model.MediaContent;
import com.kursach.movietracker.model.UserEntity;
import com.kursach.movietracker.model.WatchRecord;
import com.kursach.movietracker.model.WatchStatus;
import com.kursach.movietracker.repository.MediaContentRepository;
import com.kursach.movietracker.repository.WatchRecordRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WatchRecordService {
    private final WatchRecordRepository watchRecordRepository;
    private final MediaContentRepository mediaContentRepository;
    private final UserService userService;
    private final CatalogService catalogService;

    public WatchRecordService(
        WatchRecordRepository watchRecordRepository,
        MediaContentRepository mediaContentRepository,
        UserService userService,
        CatalogService catalogService
    ) {
        this.watchRecordRepository = watchRecordRepository;
        this.mediaContentRepository = mediaContentRepository;
        this.userService = userService;
        this.catalogService = catalogService;
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
        validateRating(request.rating());
        UserEntity user = userService.getUser(userId);
        MediaContent content = catalogService.getEntity(request.mediaContentId());

        watchRecordRepository.findByUserIdAndMediaContentId(userId, request.mediaContentId())
            .ifPresent(record -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Фильм или сериал уже есть в личном списке");
            });

        WatchStatus status = request.status() == null ? WatchStatus.PLANNED : request.status();
        WatchRecord record = watchRecordRepository.save(new WatchRecord(user, content, status, request.rating()));
        recalculateAverageRating(content.getId());
        return toResponse(record);
    }

    @Transactional
    public WatchRecordResponse update(Long userId, Long recordId, WatchRecordUpdateRequest request) {
        validateRating(request.rating());
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
        recalculateAverageRating(saved.getMediaContent().getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        WatchRecord record = watchRecordRepository.findById(recordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись просмотра не найдена"));
        if (!record.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нельзя удалить чужую запись просмотра");
        }
        Long contentId = record.getMediaContent().getId();
        watchRecordRepository.delete(record);
        recalculateAverageRating(contentId);
    }

    private void recalculateAverageRating(Long mediaContentId) {
        MediaContent content = catalogService.getEntity(mediaContentId);
        content.calculateAverageRating(watchRecordRepository.findByMediaContentId(mediaContentId));
        mediaContentRepository.save(content);
    }

    private void validateRating(Integer rating) {
        if (rating != null && (rating < 0 || rating > 10)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оценка должна быть от 0 до 10");
        }
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
