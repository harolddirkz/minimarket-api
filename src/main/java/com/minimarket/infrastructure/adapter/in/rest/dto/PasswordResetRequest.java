package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 12, max = 128, message = "La contraseña debe tener entre 12 y 128 caracteres")
        String newPassword) {
}
