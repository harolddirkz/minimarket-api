package com.minimarket.domain.model;

import java.time.Instant;
import java.util.Objects;

public record User(
        Long id,
        String username,
        String passwordHash,
        String fullName,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public User {
        username = requireText(username, "Username is required");
        passwordHash = requireText(passwordHash, "Password hash is required");
        fullName = requireText(fullName, "Full name is required");
        Objects.requireNonNull(role, "User role is required");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
