package com.kursach.movietracker.repository;

import com.kursach.movietracker.model.WatchRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {
    List<WatchRecord> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<WatchRecord> findByUserIdAndMediaContentId(Long userId, Long mediaContentId);

    List<WatchRecord> findByMediaContentId(Long mediaContentId);

    void deleteByMediaContentId(Long mediaContentId);
}
