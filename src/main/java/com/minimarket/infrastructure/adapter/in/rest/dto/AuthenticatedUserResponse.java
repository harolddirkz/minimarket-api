package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.UserRole;

public record AuthenticatedUserResponse(Long id, String username, String fullName, UserRole role) {
}
