package com.minimarket.domain.port.out;

import com.minimarket.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    int revokeActiveByUserId(Long userId, Instant revokedAt);

    int revokeActiveByFamilyId(UUID familyId, Instant revokedAt);
}
