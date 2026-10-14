package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String saleNumber,
        Long userId,
        SaleStatus status,
        BigDecimal total,
        List<SaleItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {
}
