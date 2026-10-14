package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleListResponse(
        Long id,
        String saleNumber,
        Long userId,
        SaleStatus status,
        BigDecimal total,
        Instant createdAt) {
}
