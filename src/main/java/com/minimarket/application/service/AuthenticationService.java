package com.minimarket.application.service;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.model.RefreshToken;
import com.minimarket.domain.model.AuthenticationPolicy;
import com.minimarket.domain.model.User;
import com.minimarket.domain.port.in.AuthenticatedUser;
import com.minimarket.domain.port.in.AuthenticationUseCase;
import com.minimarket.domain.port.in.LoginCommand;
import com.minimarket.domain.port.in.LoginResult;
import com.minimarket.domain.port.out.AccessTokenPort;
import com.minimarket.domain.port.out.LoginAttemptGuardPort;
import com.minimarket.domain.port.out.PasswordHashPort;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.RefreshTokenValuePort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthenticationService implements AuthenticationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHashPort passwordHashPort;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenValuePort refreshTokenValuePort;
    private final LoginAttemptGuardPort loginAttemptGuard;
    private final AuthenticationPolicy policy;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            PasswordHashPort passwordHashPort,
            AccessTokenPort accessTokenPort,
            RefreshTokenValuePort refreshTokenValuePort,
            LoginAttemptGuardPort loginAttemptGuard,
            AuthenticationPolicy policy,
            Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHashPort = passwordHashPort;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenValuePort = refreshTokenValuePort;
        this.loginAttemptGuard = loginAttemptGuard;
        this.policy = policy;
        this.clock = clock;
        this.dummyPasswordHash = passwordHashPort.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        if (command == null || command.username() == null || command.username().isBlank()
                || command.password() == null || command.password().isBlank()) {
            throw invalidCredentials();
        }

        String username = command.username().trim();
        String origin = command.origin() == null ? "unknown" : command.origin();
        loginAttemptGuard.verifyAllowed(username, origin);

        User user = userRepository.findByUsername(username).orElse(null);
        boolean passwordMatches = passwordHashPort.matches(
                command.password(), user == null ? dummyPasswordHash : user.passwordHash());
        if (user == null || !passwordMatches || !user.active()) {
            loginAttemptGuard.recordFailure(username, origin);
            log.warn("Authentication failed. reason=invalid_credentials");
            throw invalidCredentials();
        }

        AccessTokenPort.IssuedAccessToken accessToken = accessTokenPort.issue(user);
        String rawRefreshToken = refreshTokenValuePort.generate();
        Instant now = clock.instant();
        refreshTokenRepository.save(new RefreshToken(
                null,
                user.id(),
                refreshTokenValuePort.hash(rawRefreshToken),
                UUID.randomUUID(),
                now.plus(policy.refreshTokenExpiration()),
                null,
                now));
        loginAttemptGuard.clear(username, origin);
        log.info("Authentication succeeded. userId={}", user.id());

        return new LoginResult(
                accessToken.value(), rawRefreshToken, "Bearer", accessToken.expiresInSeconds(),
                new AuthenticatedUser(user.id(), user.username(), user.fullName(), user.role()));
    }

    private static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("INVALID_CREDENTIALS", "Credenciales inválidas");
    }
}
