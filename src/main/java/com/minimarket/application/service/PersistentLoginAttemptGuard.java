package com.minimarket.application.service;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.model.AuthenticationPolicy;
import com.minimarket.domain.port.out.LoginAttemptGuardPort;
import com.minimarket.domain.port.out.LoginAttemptRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.List;

@Service
public class PersistentLoginAttemptGuard implements LoginAttemptGuardPort {

    private final LoginAttemptRepositoryPort repository;
    private final AuthenticationPolicy policy;
    private final Clock clock;

    public PersistentLoginAttemptGuard(
            LoginAttemptRepositoryPort repository, AuthenticationPolicy policy, Clock clock) {
        this.repository = repository;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyAllowed(String username, String origin) {
        Instant now = clock.instant();
        boolean blocked = keys(username, origin).stream()
                .map(repository::findByKey)
                .flatMap(java.util.Optional::stream)
                .anyMatch(attempt -> attempt.blockedUntil() != null && attempt.blockedUntil().isAfter(now));
        if (blocked) {
            throw invalidCredentials();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username, String origin) {
        Instant now = clock.instant();
        for (String key : keys(username, origin)) {
            repository.recordFailureAtomically(
                    key,
                    now,
                    policy.loginAttemptWindow().toSeconds(),
                    policy.maximumLoginAttempts(),
                    policy.loginBlockDuration().toSeconds());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(String username, String origin) {
        keys(username, origin).forEach(repository::deleteByKey);
    }

    private static List<String> keys(String username, String origin) {
        return List.of(
                "user:" + sha256(username.trim().toLowerCase(Locale.ROOT)),
                "origin:" + sha256(origin == null ? "unknown" : origin));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("INVALID_CREDENTIALS", "Credenciales inválidas");
    }
}
