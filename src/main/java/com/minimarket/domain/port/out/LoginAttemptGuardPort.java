package com.minimarket.domain.port.out;

public interface LoginAttemptGuardPort {

    void verifyAllowed(String username, String origin);

    void recordFailure(String username, String origin);

    void clear(String username, String origin);
}
