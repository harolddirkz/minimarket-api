package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        @Size(max = 200, message = "El refresh token es inválido")
        String refreshToken) {
}
