package com.minimarket.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Sale(
        Long id,
        String saleNumber,
        Long userId,
        SaleStatus status,
        BigDecimal total,
        List<SaleItem> items,
        Instant createdAt,
        Instant updatedAt) {

    public Sale {
        saleNumber = requireText(saleNumber, "Sale number is required");
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(status, "Sale status is required");
        total = SalePricingPolicy.normalizeMoney(total, "Sale total");
        items = List.copyOf(Objects.requireNonNull(items, "Sale items are required"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A sale must contain at least one item");
        }
        if (items.stream().map(SaleItem::productId).distinct().count() != items.size()) {
            throw new IllegalArgumentException("A sale must not contain duplicate products");
        }
        BigDecimal expected = SalePricingPolicy.total(items);
        if (total.compareTo(expected) != 0) {
            throw new IllegalArgumentException("Sale total does not match item subtotals");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
