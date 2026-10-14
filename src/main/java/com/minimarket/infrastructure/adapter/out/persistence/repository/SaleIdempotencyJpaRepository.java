package com.minimarket.infrastructure.adapter.out.persistence.repository;

import com.minimarket.infrastructure.adapter.out.persistence.entity.SaleIdempotencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SaleIdempotencyJpaRepository extends JpaRepository<SaleIdempotencyEntity, Long> {

    Optional<SaleIdempotencyEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO sale_idempotency_keys
                (user_id, idempotency_key, request_hash, status, created_at, updated_at)
            VALUES (:userId, :idempotencyKey, :requestHash, 'PROCESSING',
                    CAST(:now AS timestamptz), CAST(:now AS timestamptz))
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int tryStart(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE sale_idempotency_keys
            SET status = 'COMPLETED', sale_id = :saleId, updated_at = CAST(:now AS timestamptz)
            WHERE user_id = :userId AND idempotency_key = :idempotencyKey
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int complete(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("saleId") Long saleId,
            @Param("now") Instant now);
}
