CREATE TABLE login_attempts (
    attempt_key VARCHAR(129) PRIMARY KEY,
    failed_count INTEGER NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    blocked_until TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_login_attempts_failed_count CHECK (failed_count >= 0),
    CONSTRAINT ck_login_attempts_blocked_until
        CHECK (blocked_until IS NULL OR blocked_until >= window_started_at)
);

CREATE INDEX idx_login_attempts_updated_at ON login_attempts (updated_at);
