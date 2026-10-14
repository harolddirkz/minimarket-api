package com.minimarket.application.service;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.CreateUserCommand;
import com.minimarket.domain.port.in.UserAdministrationUseCase;
import com.minimarket.domain.port.out.PasswordHashPort;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.UserManagementLockPort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class UserAdministrationService implements UserAdministrationUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserAdministrationService.class);

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort tokenRepository;
    private final UserManagementLockPort managementLock;
    private final PasswordHashPort passwordHashPort;
    private final Clock clock;

    public UserAdministrationService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort tokenRepository,
            UserManagementLockPort managementLock,
            PasswordHashPort passwordHashPort,
            Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.managementLock = managementLock;
        this.passwordHashPort = passwordHashPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public User create(CreateUserCommand command) {
        validateCreate(command);
        String username = command.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "El nombre de usuario ya está registrado");
        }
        Instant now = clock.instant();
        User saved = userRepository.save(new User(
                null, username, passwordHashPort.encode(command.password()), command.fullName(),
                command.role(), true, now, now));
        log.info("User created. userId={} role={}", saved.id(), saved.role());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> findAll(PageCriteria criteria) {
        if (criteria == null) {
            throw new BusinessException("INVALID_PAGINATION", "La paginación es obligatoria");
        }
        return userRepository.findAll(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return requireUser(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User findAuthenticatedUser(Long id) {
        User user = requireUser(id);
        if (!user.active()) {
            throw new AuthenticationFailedException("UNAUTHORIZED", "No autenticado");
        }
        return user;
    }

    @Override
    @Transactional
    public User updateStatus(Long actorId, Long userId, boolean active) {
        managementLock.acquireAdministratorMutationLock();
        User current = requireUserForUpdate(userId);
        if (current.active() == active) {
            return current;
        }
        if (!active && current.role() == UserRole.ADMIN && userRepository.countActiveAdministrators() <= 1) {
            throw new BusinessException(
                    "LAST_ACTIVE_ADMIN", "No se puede desactivar al último administrador activo");
        }
        User saved = userRepository.save(copy(current, current.passwordHash(), current.role(), active));
        if (!active) {
            tokenRepository.revokeActiveByUserId(userId, clock.instant());
            log.info("User deactivated. userId={} actorId={}", userId, actorId);
        } else {
            log.info("User activated. userId={} actorId={}", userId, actorId);
        }
        return saved;
    }

    @Override
    @Transactional
    public User updateRole(Long actorId, Long userId, UserRole role) {
        if (role == null) {
            throw new BusinessException("INVALID_USER_ROLE", "El rol es obligatorio");
        }
        managementLock.acquireAdministratorMutationLock();
        User current = requireUserForUpdate(userId);
        if (current.role() == role) {
            return current;
        }
        if (userId.equals(actorId)) {
            throw new BusinessException("SELF_ROLE_CHANGE", "No puede cambiar su propio rol");
        }
        if (current.active() && current.role() == UserRole.ADMIN
                && userRepository.countActiveAdministrators() <= 1) {
            throw new BusinessException(
                    "LAST_ACTIVE_ADMIN", "No se puede degradar al último administrador activo");
        }
        User saved = userRepository.save(copy(current, current.passwordHash(), role, current.active()));
        tokenRepository.revokeActiveByUserId(userId, clock.instant());
        log.info("User role changed. userId={} actorId={} role={}", userId, actorId, role);
        return saved;
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        PasswordPolicy.validate(newPassword);
        User current = requireUserForUpdate(userId);
        userRepository.save(copy(current, passwordHashPort.encode(newPassword), current.role(), current.active()));
        tokenRepository.revokeActiveByUserId(userId, clock.instant());
        log.info("User password reset. userId={}", userId);
    }

    private User requireUser(Long id) {
        if (id == null) {
            throw new BusinessException("INVALID_USER_ID", "El identificador del usuario es obligatorio");
        }
        return userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));
    }

    private User requireUserForUpdate(Long id) {
        if (id == null) {
            throw new BusinessException("INVALID_USER_ID", "El identificador del usuario es obligatorio");
        }
        return userRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ResourceNotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));
    }

    private User copy(User current, String passwordHash, UserRole role, boolean active) {
        return new User(
                current.id(), current.username(), passwordHash, current.fullName(), role, active,
                current.createdAt(), clock.instant());
    }

    private static void validateCreate(CreateUserCommand command) {
        if (command == null || command.username() == null || command.username().isBlank()) {
            throw new BusinessException("INVALID_USERNAME", "El nombre de usuario es obligatorio");
        }
        if (command.fullName() == null || command.fullName().isBlank()) {
            throw new BusinessException("INVALID_FULL_NAME", "El nombre completo es obligatorio");
        }
        if (command.role() == null) {
            throw new BusinessException("INVALID_USER_ROLE", "El rol es obligatorio");
        }
        PasswordPolicy.validate(command.password());
    }
}
