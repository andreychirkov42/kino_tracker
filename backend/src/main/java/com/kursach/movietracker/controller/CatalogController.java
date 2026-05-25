package com.kursach.movietracker.controller;

import com.kursach.movietracker.dto.MediaContentRequest;
import com.kursach.movietracker.dto.MediaContentResponse;
import com.kursach.movietracker.model.ContentType;
import com.kursach.movietracker.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<MediaContentResponse> search(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) ContentType type,
        @RequestParam(required = false) String genre,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Double minRating
    ) {
        return catalogService.search(query, type, genre, year, minRating);
    }

    @GetMapping("/{id}")
    public MediaContentResponse getById(@PathVariable Long id) {
        return catalogService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaContentResponse create(@Valid @RequestBody MediaContentRequest request) {
        return catalogService.create(request);
    }

    @PutMapping("/{id}")
    public MediaContentResponse update(@PathVariable Long id, @Valid @RequestBody MediaContentRequest request) {
        return catalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        catalogService.delete(id);
    }
}
