package com.minimarket.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalePreviewResponse(
        List<SaleItemResponse> items,
        BigDecimal total) {
}
