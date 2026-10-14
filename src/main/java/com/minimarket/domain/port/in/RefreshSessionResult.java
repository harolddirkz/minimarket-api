package com.minimarket.domain.port.in;

public record RefreshSessionResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {
}
