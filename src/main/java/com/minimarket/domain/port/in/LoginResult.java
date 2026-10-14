package com.minimarket.domain.port.in;

public record LoginResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUser user) {
}
