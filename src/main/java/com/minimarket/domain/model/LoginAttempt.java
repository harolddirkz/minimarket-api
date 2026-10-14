package com.minimarket.domain.model;

import java.time.Instant;
import java.util.Objects;

public record LoginAttempt(
        String attemptKey,
        int failedCount,
        Instant windowStartedAt,
        Instant blockedUntil,
        Instant updatedAt) {

    public LoginAttempt {
        if (attemptKey == null || attemptKey.isBlank()) {
            throw new IllegalArgumentException("Login attempt key is required");
        }
        if (failedCount < 0) {
            throw new IllegalArgumentException("Failed count cannot be negative");
        }
        Objects.requireNonNull(windowStartedAt, "Window start is required");
        Objects.requireNonNull(updatedAt, "Update date is required");
    }
}
