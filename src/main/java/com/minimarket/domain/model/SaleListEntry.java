package com.minimarket.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleListEntry(
        Long id,
        String saleNumber,
        Long userId,
        SaleStatus status,
        BigDecimal total,
        Instant createdAt) {
}
