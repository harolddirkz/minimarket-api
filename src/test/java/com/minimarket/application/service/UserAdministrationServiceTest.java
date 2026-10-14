package com.minimarket.application.service;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.CreateUserCommand;
import com.minimarket.domain.port.out.PasswordHashPort;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.UserManagementLockPort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");

    @Mock UserRepositoryPort userRepository;
    @Mock RefreshTokenRepositoryPort tokenRepository;
    @Mock UserManagementLockPort managementLock;
    @Mock PasswordHashPort passwordHashPort;

    private UserAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new UserAdministrationService(
                userRepository, tokenRepository, managementLock, passwordHashPort,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCreateUserWithEncodedPassword() {
        when(userRepository.existsByUsername("familiar01")).thenReturn(false);
        when(passwordHashPort.encode("StrongPassword1!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        User result = service.create(new CreateUserCommand(
                " familiar01 ", "StrongPassword1!", "Familiar Uno", UserRole.VENDEDOR));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(result.role()).isEqualTo(UserRole.VENDEDOR);
    }

    @Test
    void shouldRejectDuplicatedUsername() {
        when(userRepository.existsByUsername("familiar01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserCommand(
                "familiar01", "StrongPassword1!", "Familiar Uno", UserRole.VENDEDOR)))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("USERNAME_ALREADY_EXISTS");
    }

    @Test
    void shouldRejectWeakPassword() {
        assertThatThrownBy(() -> service.create(new CreateUserCommand(
                "familiar01", "weak", "Familiar Uno", UserRole.VENDEDOR)))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("WEAK_PASSWORD");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateUserAndRevokeSessions() {
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vendor()));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.updateStatus(1L, 2L, false);

        assertThat(result.active()).isFalse();
        verify(tokenRepository).revokeActiveByUserId(2L, NOW);
    }

    @Test
    void shouldProtectLastActiveAdministrator() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin()));
        when(userRepository.countActiveAdministrators()).thenReturn(1L);

        assertThatThrownBy(() -> service.updateStatus(1L, 1L, false))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("LAST_ACTIVE_ADMIN");
    }

    @Test
    void shouldRejectChangingOwnRole() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin()));

        assertThatThrownBy(() -> service.updateRole(1L, 1L, UserRole.VENDEDOR))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SELF_ROLE_CHANGE");
    }

    @Test
    void shouldResetPasswordAndRevokeSessions() {
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(vendor()));
        when(passwordHashPort.encode("AnotherStrong1!")).thenReturn("new-hash");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetPassword(2L, "AnotherStrong1!");

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                user -> user.passwordHash().equals("new-hash")));
        verify(tokenRepository).revokeActiveByUserId(2L, NOW);
    }

    private static User admin() {
        return new User(1L, "admin", "hash", "Admin", UserRole.ADMIN, true, NOW, NOW);
    }

    private static User vendor() {
        return new User(2L, "vendor", "hash", "Vendor", UserRole.VENDEDOR, true, NOW, NOW);
    }

    private static User withId(User user) {
        return new User(
                10L, user.username(), user.passwordHash(), user.fullName(), user.role(), user.active(),
                user.createdAt(), user.updatedAt());
    }
}
