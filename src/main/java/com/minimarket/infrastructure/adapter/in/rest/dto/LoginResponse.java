package com.minimarket.infrastructure.adapter.in.rest.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserResponse user) {
}
