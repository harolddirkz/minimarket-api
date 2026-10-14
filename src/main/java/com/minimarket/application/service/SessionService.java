package com.minimarket.application.service;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.model.AuthenticationPolicy;
import com.minimarket.domain.model.RefreshToken;
import com.minimarket.domain.model.User;
import com.minimarket.domain.port.in.LogoutCommand;
import com.minimarket.domain.port.in.RefreshSessionCommand;
import com.minimarket.domain.port.in.RefreshSessionResult;
import com.minimarket.domain.port.in.SessionUseCase;
import com.minimarket.domain.port.out.AccessTokenPort;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.RefreshTokenValuePort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class SessionService implements SessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final RefreshTokenRepositoryPort tokenRepository;
    private final UserRepositoryPort userRepository;
    private final RefreshTokenValuePort tokenValuePort;
    private final AccessTokenPort accessTokenPort;
    private final AuthenticationPolicy policy;
    private final Clock clock;

    public SessionService(
            RefreshTokenRepositoryPort tokenRepository,
            UserRepositoryPort userRepository,
            RefreshTokenValuePort tokenValuePort,
            AccessTokenPort accessTokenPort,
            AuthenticationPolicy policy,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenValuePort = tokenValuePort;
        this.accessTokenPort = accessTokenPort;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public RefreshSessionResult refresh(RefreshSessionCommand command) {
        String rawToken = requireRefreshToken(command == null ? null : command.refreshToken());
        RefreshToken current = tokenRepository.findByTokenHashForUpdate(tokenValuePort.hash(rawToken))
                .orElseThrow(SessionService::invalidRefreshToken);
        Instant now = clock.instant();

        if (current.revokedAt() != null) {
            tokenRepository.revokeActiveByFamilyId(current.familyId(), now);
            log.warn("Refresh token reuse detected. userId={} familyId={}", current.userId(), current.familyId());
            throw invalidRefreshToken();
        }
        if (!current.expiresAt().isAfter(now)) {
            tokenRepository.save(revoked(current, now));
            throw invalidRefreshToken();
        }

        User user = userRepository.findById(current.userId()).orElse(null);
        if (user == null || !user.active()) {
            tokenRepository.revokeActiveByFamilyId(current.familyId(), now);
            throw invalidRefreshToken();
        }

        tokenRepository.save(revoked(current, now));
        String newRawToken = tokenValuePort.generate();
        tokenRepository.save(new RefreshToken(
                null,
                user.id(),
                tokenValuePort.hash(newRawToken),
                current.familyId(),
                now.plus(policy.refreshTokenExpiration()),
                null,
                now));
        AccessTokenPort.IssuedAccessToken accessToken = accessTokenPort.issue(user);
        log.info("Session refreshed. userId={}", user.id());
        return new RefreshSessionResult(
                accessToken.value(), newRawToken, "Bearer", accessToken.expiresInSeconds());
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        if (command == null || command.authenticatedUserId() == null) {
            throw invalidRefreshToken();
        }
        String rawToken = requireRefreshToken(command.refreshToken());
        RefreshToken token = tokenRepository.findByTokenHashForUpdate(tokenValuePort.hash(rawToken))
                .orElseThrow(SessionService::invalidRefreshToken);
        if (!token.userId().equals(command.authenticatedUserId())) {
            throw invalidRefreshToken();
        }
        if (token.revokedAt() == null) {
            tokenRepository.save(revoked(token, clock.instant()));
        }
        log.info("Session closed. userId={}", command.authenticatedUserId());
    }

    private static RefreshToken revoked(RefreshToken token, Instant revokedAt) {
        return new RefreshToken(
                token.id(), token.userId(), token.tokenHash(), token.familyId(), token.expiresAt(),
                revokedAt, token.createdAt());
    }

    private static String requireRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw invalidRefreshToken();
        }
        return token;
    }

    private static AuthenticationFailedException invalidRefreshToken() {
        return new AuthenticationFailedException("INVALID_REFRESH_TOKEN", "Refresh token inválido");
    }
}
