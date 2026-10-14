package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.SaleIdempotency;
import com.minimarket.domain.port.out.SaleIdempotencyRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.entity.SaleIdempotencyEntity;
import com.minimarket.infrastructure.adapter.out.persistence.repository.SaleIdempotencyJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SaleIdempotencyPersistenceAdapter implements SaleIdempotencyRepositoryPort {

    private final SaleIdempotencyJpaRepository repository;

    public SaleIdempotencyPersistenceAdapter(SaleIdempotencyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean tryStart(Long userId, String idempotencyKey, String requestHash, Instant now) {
        return repository.tryStart(userId, idempotencyKey, requestHash, now) == 1;
    }

    @Override
    public Optional<SaleIdempotency> find(Long userId, String idempotencyKey) {
        return repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(SaleIdempotencyPersistenceAdapter::toDomain);
    }

    @Override
    public void complete(Long userId, String idempotencyKey, Long saleId, Instant now) {
        int updated = repository.complete(userId, idempotencyKey, saleId, now);
        if (updated != 1) {
            throw new IllegalStateException("Idempotency operation is not in PROCESSING state");
        }
    }

    private static SaleIdempotency toDomain(SaleIdempotencyEntity entity) {
        return new SaleIdempotency(
                entity.getUserId(),
                entity.getIdempotencyKey(),
                entity.getRequestHash(),
                entity.getStatus(),
                entity.getSaleId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
