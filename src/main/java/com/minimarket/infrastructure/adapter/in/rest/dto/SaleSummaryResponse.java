package com.minimarket.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

public record SaleSummaryResponse(
        long totalSales,
        BigDecimal totalAmount,
        String currency) {
}
