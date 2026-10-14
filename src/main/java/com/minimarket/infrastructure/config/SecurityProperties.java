package com.minimarket.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        Jwt jwt,
        Duration refreshTokenExpiration,
        LoginAttempts loginAttempts,
        BootstrapAdmin bootstrapAdmin,
        boolean requireHttps,
        boolean documentationPublic) {

    public record Jwt(String issuer, String secretBase64, Duration accessTokenExpiration) {
    }

    public record LoginAttempts(int maximum, Duration window, Duration blockDuration) {
    }

    public record BootstrapAdmin(String username, String password, String fullName) {
    }
}
