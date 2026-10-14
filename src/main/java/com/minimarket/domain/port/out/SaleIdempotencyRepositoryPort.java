package com.minimarket.domain.port.out;

import com.minimarket.domain.model.SaleIdempotency;

import java.time.Instant;
import java.util.Optional;

public interface SaleIdempotencyRepositoryPort {

    boolean tryStart(Long userId, String idempotencyKey, String requestHash, Instant now);

    Optional<SaleIdempotency> find(Long userId, String idempotencyKey);

    void complete(Long userId, String idempotencyKey, Long saleId, Instant now);
}
