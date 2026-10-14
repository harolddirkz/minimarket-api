package com.minimarket.domain.port.in;

public record LogoutCommand(Long authenticatedUserId, String refreshToken) {
}
