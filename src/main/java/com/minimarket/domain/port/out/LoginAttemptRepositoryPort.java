package com.minimarket.domain.port.out;

import com.minimarket.domain.model.LoginAttempt;

import java.util.Optional;
import java.time.Instant;

public interface LoginAttemptRepositoryPort {

    Optional<LoginAttempt> findByKeyForUpdate(String attemptKey);

    Optional<LoginAttempt> findByKey(String attemptKey);

    LoginAttempt save(LoginAttempt attempt);

    void recordFailureAtomically(
            String attemptKey,
            Instant now,
            long windowSeconds,
            int maximumAttempts,
            long blockSeconds);

    void deleteByKey(String attemptKey);
}
