package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.RefreshToken;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.minimarket.infrastructure.adapter.out.persistence.mapper.RefreshTokenPersistenceMapper;
import com.minimarket.infrastructure.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.minimarket.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository tokenRepository;
    private final UserJpaRepository userRepository;

    public RefreshTokenPersistenceAdapter(
            RefreshTokenJpaRepository tokenRepository,
            UserJpaRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        UserEntity user = userRepository.getReferenceById(refreshToken.userId());
        return RefreshTokenPersistenceMapper.toDomain(tokenRepository.save(
                RefreshTokenPersistenceMapper.toEntity(refreshToken, user)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return tokenRepository.findByTokenHashForUpdate(tokenHash)
                .map(RefreshTokenPersistenceMapper::toDomain);
    }

    @Override
    public int revokeActiveByUserId(Long userId, Instant revokedAt) {
        return tokenRepository.revokeActiveByUserId(userId, revokedAt);
    }

    @Override
    public int revokeActiveByFamilyId(UUID familyId, Instant revokedAt) {
        return tokenRepository.revokeActiveByFamilyId(familyId, revokedAt);
    }
}
