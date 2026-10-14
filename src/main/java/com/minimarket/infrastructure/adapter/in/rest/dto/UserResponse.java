package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
