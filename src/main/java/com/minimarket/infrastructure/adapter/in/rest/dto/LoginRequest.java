package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80, message = "El usuario debe tener como máximo 80 caracteres")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 200, message = "La contraseña es demasiado larga")
        String password) {
}
