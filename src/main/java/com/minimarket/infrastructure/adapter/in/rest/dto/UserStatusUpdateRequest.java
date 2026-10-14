package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(@NotNull(message = "El estado es obligatorio") Boolean active) {
}
