package com.minimarket.application.service;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.model.RefreshToken;
import com.minimarket.domain.model.AuthenticationPolicy;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.LoginCommand;
import com.minimarket.domain.port.in.LoginResult;
import com.minimarket.domain.port.out.AccessTokenPort;
import com.minimarket.domain.port.out.LoginAttemptGuardPort;
import com.minimarket.domain.port.out.PasswordHashPort;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.RefreshTokenValuePort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");

    @Mock UserRepositoryPort userRepository;
    @Mock RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock PasswordHashPort passwordHashPort;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenValuePort refreshTokenValuePort;
    @Mock LoginAttemptGuardPort loginAttemptGuard;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        when(passwordHashPort.encode(any())).thenReturn("dummy-hash");
        AuthenticationPolicy policy = new AuthenticationPolicy(
                Duration.ofDays(30), 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        service = new AuthenticationService(
                userRepository, refreshTokenRepository, passwordHashPort, accessTokenPort,
                refreshTokenValuePort, loginAttemptGuard, policy,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldLoginAndPersistOnlyRefreshTokenHash() {
        User user = activeUser();
        when(userRepository.findByUsername("admin01")).thenReturn(Optional.of(user));
        when(passwordHashPort.matches("StrongPassword1!", user.passwordHash())).thenReturn(true);
        when(accessTokenPort.issue(user)).thenReturn(new AccessTokenPort.IssuedAccessToken("jwt", 900));
        when(refreshTokenValuePort.generate()).thenReturn("raw-refresh-token");
        when(refreshTokenValuePort.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResult result = service.login(new LoginCommand(" admin01 ", "StrongPassword1!", "127.0.0.1"));

        assertThat(result.accessToken()).isEqualTo("jwt");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.expiresIn()).isEqualTo(900);
        verify(refreshTokenRepository).save(org.mockito.ArgumentMatchers.argThat(token ->
                "hashed-refresh-token".equals(token.tokenHash())
                        && !token.tokenHash().equals(result.refreshToken())
                        && token.userId().equals(user.id())));
        verify(loginAttemptGuard).clear("admin01", "127.0.0.1");
    }

    @Test
    void shouldReturnGenericErrorForIncorrectPassword() {
        User user = activeUser();
        when(userRepository.findByUsername("admin01")).thenReturn(Optional.of(user));
        when(passwordHashPort.matches("wrong", user.passwordHash())).thenReturn(false);

        assertInvalidCredentials(new LoginCommand("admin01", "wrong", "127.0.0.1"));
        verify(loginAttemptGuard).recordFailure("admin01", "127.0.0.1");
    }

    @Test
    void shouldReturnSameGenericErrorForMissingUser() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(passwordHashPort.matches("wrong", "dummy-hash")).thenReturn(false);

        assertInvalidCredentials(new LoginCommand("unknown", "wrong", "127.0.0.1"));
        verify(loginAttemptGuard).recordFailure("unknown", "127.0.0.1");
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = new User(1L, "admin01", "stored-hash", "Admin", UserRole.ADMIN, false, NOW, NOW);
        when(userRepository.findByUsername("admin01")).thenReturn(Optional.of(user));
        when(passwordHashPort.matches("StrongPassword1!", user.passwordHash())).thenReturn(true);

        assertInvalidCredentials(new LoginCommand("admin01", "StrongPassword1!", "127.0.0.1"));
        verify(loginAttemptGuard).recordFailure("admin01", "127.0.0.1");
    }

    private void assertInvalidCredentials(LoginCommand command) {
        assertThatThrownBy(() -> service.login(command))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas")
                .extracting("code")
                .isEqualTo("INVALID_CREDENTIALS");
    }

    private static User activeUser() {
        return new User(1L, "admin01", "stored-hash", "Admin", UserRole.ADMIN, true, NOW, NOW);
    }
}
