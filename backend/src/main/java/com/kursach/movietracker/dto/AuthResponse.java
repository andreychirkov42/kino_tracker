package com.kursach.movietracker.dto;

public record AuthResponse(
    Long id,
    String username,
    String email,
    String role
) {
}
