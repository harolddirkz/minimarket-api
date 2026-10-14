package com.minimarket.infrastructure.adapter.out.persistence.mapper;

import com.minimarket.domain.model.RefreshToken;
import com.minimarket.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import com.minimarket.infrastructure.adapter.out.persistence.entity.UserEntity;

public final class RefreshTokenPersistenceMapper {

    private RefreshTokenPersistenceMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
                entity.getId(), entity.getUser().getId(), entity.getTokenHash(), entity.getFamilyId(),
                entity.getExpiresAt(), entity.getRevokedAt(), entity.getCreatedAt());
    }

    public static RefreshTokenEntity toEntity(RefreshToken token, UserEntity user) {
        return new RefreshTokenEntity(
                token.id(), user, token.tokenHash(), token.familyId(), token.expiresAt(),
                token.revokedAt(), token.createdAt());
    }
}
