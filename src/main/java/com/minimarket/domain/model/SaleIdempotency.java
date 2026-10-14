package com.minimarket.domain.model;

import java.time.Instant;
import java.util.Objects;

public record SaleIdempotency(
        Long userId,
        String idempotencyKey,
        String requestHash,
        SaleIdempotencyStatus status,
        Long saleId,
        Instant createdAt,
        Instant updatedAt) {

    public SaleIdempotency {
        Objects.requireNonNull(userId, "User id is required");
        idempotencyKey = requireText(idempotencyKey, "Idempotency key is required");
        requestHash = requireText(requestHash, "Request hash is required");
        Objects.requireNonNull(status, "Idempotency status is required");
        if (status == SaleIdempotencyStatus.COMPLETED && saleId == null) {
            throw new IllegalArgumentException("A completed idempotency operation requires a sale id");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
