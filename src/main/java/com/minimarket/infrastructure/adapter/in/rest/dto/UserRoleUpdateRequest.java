package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(@NotNull(message = "El rol es obligatorio") UserRole role) {
}
