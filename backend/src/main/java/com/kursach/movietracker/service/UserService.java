package com.kursach.movietracker.service;

import com.kursach.movietracker.dto.AuthResponse;
import com.kursach.movietracker.dto.LoginRequest;
import com.kursach.movietracker.dto.RegisterRequest;
import com.kursach.movietracker.model.Role;
import com.kursach.movietracker.model.UserEntity;
import com.kursach.movietracker.repository.RoleRepository;
import com.kursach.movietracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким логином уже существует");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с такой почтой уже существует");
        }

        Role role = roleRepository.findByName("USER")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Роль USER не найдена"));
        UserEntity user = new UserEntity(
            request.username(),
            request.email(),
            passwordEncoder.encode(request.password()),
            role
        );
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    public AuthResponse toResponse(UserEntity user) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getName());
    }
}
