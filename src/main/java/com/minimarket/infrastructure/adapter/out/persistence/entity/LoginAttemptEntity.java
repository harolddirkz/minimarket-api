package com.minimarket.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttemptEntity {

    @Id
    @Column(name = "attempt_key", length = 129)
    private String attemptKey;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LoginAttemptEntity() {
    }

    public LoginAttemptEntity(String attemptKey, int failedCount, Instant windowStartedAt,
            Instant blockedUntil, Instant updatedAt) {
        this.attemptKey = attemptKey;
        this.failedCount = failedCount;
        this.windowStartedAt = windowStartedAt;
        this.blockedUntil = blockedUntil;
        this.updatedAt = updatedAt;
    }

    public String getAttemptKey() { return attemptKey; }
    public int getFailedCount() { return failedCount; }
    public Instant getWindowStartedAt() { return windowStartedAt; }
    public Instant getBlockedUntil() { return blockedUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
}
