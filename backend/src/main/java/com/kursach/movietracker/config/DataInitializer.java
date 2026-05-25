package com.kursach.movietracker.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kursach.movietracker.dto.MediaContentRequest;
import com.kursach.movietracker.dto.WatchRecordRequest;
import com.kursach.movietracker.model.ContentType;
import com.kursach.movietracker.model.Role;
import com.kursach.movietracker.model.UserEntity;
import com.kursach.movietracker.model.WatchStatus;
import com.kursach.movietracker.repository.MediaContentRepository;
import com.kursach.movietracker.repository.RoleRepository;
import com.kursach.movietracker.repository.UserRepository;
import com.kursach.movietracker.repository.WatchRecordRepository;
import com.kursach.movietracker.service.CatalogService;
import com.kursach.movietracker.service.WatchRecordService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final String SEED_PATH = "seed/kinohub-catalog.json";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MediaContentRepository mediaContentRepository;
    private final WatchRecordRepository watchRecordRepository;
    private final CatalogService catalogService;
    private final WatchRecordService watchRecordService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataInitializer(
        RoleRepository roleRepository,
        UserRepository userRepository,
        MediaContentRepository mediaContentRepository,
        WatchRecordRepository watchRecordRepository,
        CatalogService catalogService,
        WatchRecordService watchRecordService,
        PasswordEncoder passwordEncoder,
        ObjectMapper objectMapper
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.mediaContentRepository = mediaContentRepository;
        this.watchRecordRepository = watchRecordRepository;
        this.catalogService = catalogService;
        this.watchRecordService = watchRecordService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> roleRepository.save(new Role("USER")));
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(new Role("ADMIN")));

        if (!userRepository.existsByUsername("student")) {
            userRepository.save(new UserEntity("student", "student@mail.ru", passwordEncoder.encode("123456"), userRole));
        }
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new UserEntity("admin", "admin@mail.ru", passwordEncoder.encode("admin123"), adminRole));
        }

        seedCatalogFromJson();
        seedStudentWatchlist();
    }

    private void seedCatalogFromJson() {
        for (SeedMediaItem item : readSeedItems()) {
            if (mediaContentRepository.existsByTitleAndReleaseYear(item.title(), item.releaseYear())) {
                continue;
            }
            catalogService.create(new MediaContentRequest(
                item.title(),
                item.originalTitle(),
                item.contentType(),
                item.releaseYear(),
                item.duration(),
                item.description(),
                item.director(),
                item.mood(),
                item.posterUrl(),
                item.sourceUrl(),
                item.genres()
            ));
        }
    }

    private List<SeedMediaItem> readSeedItems() {
        ClassPathResource resource = new ClassPathResource(SEED_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read seed catalog: " + SEED_PATH, exception);
        }
    }

    private void seedStudentWatchlist() {
        UserEntity student = userRepository.findByUsername("student").orElseThrow();
        if (!watchRecordRepository.findByUserIdOrderByAddedAtDesc(student.getId()).isEmpty()) {
            return;
        }

        addRecord(student.getId(), 1L, WatchStatus.WATCHED, 8);
        addRecord(student.getId(), 3L, WatchStatus.WATCHED, 9);
        addRecord(student.getId(), 4L, WatchStatus.WATCHED, 7);
        addRecord(student.getId(), 9L, WatchStatus.FAVORITE, 9);
        addRecord(student.getId(), 10L, WatchStatus.PLANNED, null);
    }

    private void addRecord(Long userId, Long mediaContentId, WatchStatus status, Integer rating) {
        if (mediaContentRepository.existsById(mediaContentId)) {
            watchRecordService.add(userId, new WatchRecordRequest(mediaContentId, status, rating));
        }
    }

    public record SeedMediaItem(
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
        List<String> genres
    ) {
    }
}
