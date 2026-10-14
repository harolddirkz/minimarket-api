package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.LoginAttempt;
import com.minimarket.domain.port.out.LoginAttemptRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.entity.LoginAttemptEntity;
import com.minimarket.infrastructure.adapter.out.persistence.repository.LoginAttemptJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.time.Instant;

@Component
public class LoginAttemptPersistenceAdapter implements LoginAttemptRepositoryPort {

    private final LoginAttemptJpaRepository repository;

    public LoginAttemptPersistenceAdapter(LoginAttemptJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LoginAttempt> findByKeyForUpdate(String attemptKey) {
        return repository.findByKeyForUpdate(attemptKey).map(LoginAttemptPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<LoginAttempt> findByKey(String attemptKey) {
        return repository.findById(attemptKey).map(LoginAttemptPersistenceAdapter::toDomain);
    }

    @Override
    public LoginAttempt save(LoginAttempt attempt) {
        return toDomain(repository.save(new LoginAttemptEntity(
                attempt.attemptKey(), attempt.failedCount(), attempt.windowStartedAt(),
                attempt.blockedUntil(), attempt.updatedAt())));
    }

    @Override
    public void recordFailureAtomically(
            String attemptKey,
            Instant now,
            long windowSeconds,
            int maximumAttempts,
            long blockSeconds) {
        repository.recordFailureAtomically(
                attemptKey, now, windowSeconds, maximumAttempts, blockSeconds);
    }

    @Override
    public void deleteByKey(String attemptKey) {
        repository.deleteById(attemptKey);
    }

    private static LoginAttempt toDomain(LoginAttemptEntity entity) {
        return new LoginAttempt(
                entity.getAttemptKey(), entity.getFailedCount(), entity.getWindowStartedAt(),
                entity.getBlockedUntil(), entity.getUpdatedAt());
    }
}
