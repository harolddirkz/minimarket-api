package com.minimarket.infrastructure.adapter.in.rest.dto;

public record RefreshSessionResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {
}
