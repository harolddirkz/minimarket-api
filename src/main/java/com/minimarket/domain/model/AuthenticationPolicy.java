package com.minimarket.domain.model;

import java.time.Duration;
import java.util.Objects;

public record AuthenticationPolicy(
        Duration refreshTokenExpiration,
        int maximumLoginAttempts,
        Duration loginAttemptWindow,
        Duration loginBlockDuration) {

    public AuthenticationPolicy {
        Objects.requireNonNull(refreshTokenExpiration, "Refresh token expiration is required");
        Objects.requireNonNull(loginAttemptWindow, "Login attempt window is required");
        Objects.requireNonNull(loginBlockDuration, "Login block duration is required");
        if (refreshTokenExpiration.isNegative() || refreshTokenExpiration.isZero()
                || loginAttemptWindow.isNegative() || loginAttemptWindow.isZero()
                || loginBlockDuration.isNegative() || loginBlockDuration.isZero()
                || maximumLoginAttempts < 1) {
            throw new IllegalArgumentException("Authentication policy values must be positive");
        }
    }
}
