package com.minimarket.infrastructure.adapter.out.persistence.repository;

import com.minimarket.infrastructure.adapter.out.persistence.entity.LoginAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.Instant;

public interface LoginAttemptJpaRepository extends JpaRepository<LoginAttemptEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from LoginAttemptEntity attempt where attempt.attemptKey = :attemptKey")
    Optional<LoginAttemptEntity> findByKeyForUpdate(@Param("attemptKey") String attemptKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO login_attempts
                (attempt_key, failed_count, window_started_at, blocked_until, updated_at)
            VALUES
                (:attemptKey, 1, CAST(:now AS timestamptz),
                 CASE WHEN :maximumAttempts <= 1
                      THEN CAST(:now AS timestamptz) + (:blockSeconds * INTERVAL '1 second') ELSE NULL END,
                 CAST(:now AS timestamptz))
            ON CONFLICT (attempt_key) DO UPDATE SET
                failed_count = CASE
                    WHEN login_attempts.window_started_at + (:windowSeconds * INTERVAL '1 second') <= CAST(:now AS timestamptz)
                    THEN 1 ELSE login_attempts.failed_count + 1 END,
                window_started_at = CASE
                    WHEN login_attempts.window_started_at + (:windowSeconds * INTERVAL '1 second') <= CAST(:now AS timestamptz)
                    THEN CAST(:now AS timestamptz) ELSE login_attempts.window_started_at END,
                blocked_until = CASE
                    WHEN login_attempts.blocked_until > CAST(:now AS timestamptz) THEN login_attempts.blocked_until
                    WHEN (CASE
                        WHEN login_attempts.window_started_at + (:windowSeconds * INTERVAL '1 second') <= CAST(:now AS timestamptz)
                        THEN 1 ELSE login_attempts.failed_count + 1 END) >= :maximumAttempts
                    THEN CAST(:now AS timestamptz) + (:blockSeconds * INTERVAL '1 second')
                    ELSE NULL END,
                updated_at = CAST(:now AS timestamptz)
            """, nativeQuery = true)
    void recordFailureAtomically(
            @Param("attemptKey") String attemptKey,
            @Param("now") Instant now,
            @Param("windowSeconds") long windowSeconds,
            @Param("maximumAttempts") int maximumAttempts,
            @Param("blockSeconds") long blockSeconds);
}
