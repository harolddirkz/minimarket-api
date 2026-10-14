package com.minimarket.application.service;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.model.AuthenticationPolicy;
import com.minimarket.domain.model.RefreshToken;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.LogoutCommand;
import com.minimarket.domain.port.in.RefreshSessionCommand;
import com.minimarket.domain.port.in.RefreshSessionResult;
import com.minimarket.domain.port.out.AccessTokenPort;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.RefreshTokenValuePort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");
    private static final UUID FAMILY_ID = UUID.fromString("5f62ebd6-01c3-43bd-8187-e8347cfae777");

    @Mock RefreshTokenRepositoryPort tokenRepository;
    @Mock UserRepositoryPort userRepository;
    @Mock RefreshTokenValuePort tokenValuePort;
    @Mock AccessTokenPort accessTokenPort;

    private SessionService service;

    @BeforeEach
    void setUp() {
        service = new SessionService(
                tokenRepository, userRepository, tokenValuePort, accessTokenPort,
                new AuthenticationPolicy(Duration.ofDays(30), 5, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRotateRefreshTokenAndKeepFamily() {
        RefreshToken current = token(null);
        User user = user(true);
        when(tokenValuePort.hash("old-raw")).thenReturn("old-hash");
        when(tokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(current));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tokenValuePort.generate()).thenReturn("new-raw");
        when(tokenValuePort.hash("new-raw")).thenReturn("new-hash");
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accessTokenPort.issue(user)).thenReturn(new AccessTokenPort.IssuedAccessToken("new-jwt", 900));

        RefreshSessionResult result = service.refresh(new RefreshSessionCommand("old-raw"));

        assertThat(result.refreshToken()).isEqualTo("new-raw");
        assertThat(result.accessToken()).isEqualTo("new-jwt");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).revokedAt()).isEqualTo(NOW);
        assertThat(captor.getAllValues().get(1).familyId()).isEqualTo(FAMILY_ID);
        assertThat(captor.getAllValues().get(1).tokenHash()).isEqualTo("new-hash");
    }

    @Test
    void shouldRevokeFamilyWhenRotatedTokenIsReused() {
        when(tokenValuePort.hash("reused")).thenReturn("old-hash");
        when(tokenRepository.findByTokenHashForUpdate("old-hash"))
                .thenReturn(Optional.of(token(NOW.minusSeconds(1))));

        assertInvalid(() -> service.refresh(new RefreshSessionCommand("reused")));

        verify(tokenRepository).revokeActiveByFamilyId(FAMILY_ID, NOW);
        verify(accessTokenPort, never()).issue(any());
    }

    @Test
    void shouldRevokeExpiredToken() {
        RefreshToken expired = new RefreshToken(
                10L, 1L, "old-hash", FAMILY_ID, NOW.minusSeconds(1), null, NOW.minusSeconds(3600));
        when(tokenValuePort.hash("expired")).thenReturn("old-hash");
        when(tokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(expired));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertInvalid(() -> service.refresh(new RefreshSessionCommand("expired")));

        verify(tokenRepository).save(org.mockito.ArgumentMatchers.argThat(value -> value.revokedAt().equals(NOW)));
    }

    @Test
    void shouldRevokeFamilyForInactiveUser() {
        when(tokenValuePort.hash("raw")).thenReturn("old-hash");
        when(tokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(token(null)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(false)));

        assertInvalid(() -> service.refresh(new RefreshSessionCommand("raw")));

        verify(tokenRepository).revokeActiveByFamilyId(FAMILY_ID, NOW);
    }

    @Test
    void shouldRejectLogoutForAnotherUsersSession() {
        when(tokenValuePort.hash("raw")).thenReturn("old-hash");
        when(tokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(token(null)));

        assertInvalid(() -> service.logout(new LogoutCommand(2L, "raw")));

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void shouldRevokeOwnSessionOnLogout() {
        when(tokenValuePort.hash("raw")).thenReturn("old-hash");
        when(tokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(token(null)));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.logout(new LogoutCommand(1L, "raw"));

        verify(tokenRepository).save(org.mockito.ArgumentMatchers.argThat(value -> value.revokedAt().equals(NOW)));
    }

    private static RefreshToken token(Instant revokedAt) {
        return new RefreshToken(10L, 1L, "old-hash", FAMILY_ID,
                NOW.plus(Duration.ofDays(30)), revokedAt, NOW.minusSeconds(60));
    }

    private static User user(boolean active) {
        return new User(1L, "admin01", "hash", "Admin", UserRole.ADMIN, active, NOW, NOW);
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }
}
