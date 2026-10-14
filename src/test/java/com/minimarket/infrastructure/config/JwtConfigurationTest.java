package com.minimarket.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {

    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void shouldValidateCorrectlySignedAccessToken() {
        SecurityProperties properties = properties("minimarket-api", "0123456789abcdef0123456789abcdef");
        SecretKey key = configuration.jwtSecretKey(properties);
        String token = token(configuration.jwtEncoder(key), "minimarket-api",
                Instant.now().minusSeconds(1), Instant.now().plusSeconds(300));

        assertThat(configuration.jwtDecoder(key, properties).decode(token).getSubject()).isEqualTo("admin01");
    }

    @Test
    void shouldRejectExpiredToken() {
        SecurityProperties properties = properties("minimarket-api", "0123456789abcdef0123456789abcdef");
        SecretKey key = configuration.jwtSecretKey(properties);
        String token = token(configuration.jwtEncoder(key), "minimarket-api",
                Instant.now().minusSeconds(600), Instant.now().minusSeconds(300));

        assertRejected(configuration.jwtDecoder(key, properties), token);
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        SecurityProperties properties = properties("minimarket-api", "0123456789abcdef0123456789abcdef");
        SecretKey validationKey = configuration.jwtSecretKey(properties);
        SecretKey otherKey = configuration.jwtSecretKey(
                properties("minimarket-api", "abcdef0123456789abcdef0123456789"));
        String token = token(configuration.jwtEncoder(otherKey), "minimarket-api",
                Instant.now().minusSeconds(1), Instant.now().plusSeconds(300));

        assertRejected(configuration.jwtDecoder(validationKey, properties), token);
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {
        SecurityProperties properties = properties("minimarket-api", "0123456789abcdef0123456789abcdef");
        SecretKey key = configuration.jwtSecretKey(properties);
        String token = token(configuration.jwtEncoder(key), "another-issuer",
                Instant.now().minusSeconds(1), Instant.now().plusSeconds(300));

        assertRejected(configuration.jwtDecoder(key, properties), token);
    }

    private static String token(JwtEncoder encoder, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("admin01")
                .id("test-jti")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("user_id", 1L)
                .claim("role", "ADMIN")
                .claim("token_type", "access")
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }

    private static SecurityProperties properties(String issuer, String secret) {
        return new SecurityProperties(
                new SecurityProperties.Jwt(
                        issuer,
                        Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8)),
                        Duration.ofMinutes(15)),
                Duration.ofDays(30),
                new SecurityProperties.LoginAttempts(5, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                new SecurityProperties.BootstrapAdmin("", "", "Administrador inicial"),
                false,
                true);
    }

    private static void assertRejected(JwtDecoder decoder, String token) {
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
