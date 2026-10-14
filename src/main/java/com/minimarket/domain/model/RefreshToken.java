package com.minimarket.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefreshToken(
        Long id,
        Long userId,
        String tokenHash,
        UUID familyId,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt) {

    public RefreshToken {
        Objects.requireNonNull(userId, "User id is required");
        tokenHash = requireText(tokenHash, "Token hash is required");
        Objects.requireNonNull(familyId, "Token family id is required");
        Objects.requireNonNull(expiresAt, "Token expiration is required");
        Objects.requireNonNull(createdAt, "Token creation date is required");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Token expiration must be after its creation date");
        }
        if (revokedAt != null && revokedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Token revocation cannot be before its creation date");
        }
    }

    public boolean isUsableAt(Instant instant) {
        Objects.requireNonNull(instant, "Validation date is required");
        return revokedAt == null && expiresAt.isAfter(instant);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
