package com.minimarket.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public final class SalePricingPolicy {

    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 3;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private SalePricingPolicy() {
    }

    public static BigDecimal normalizeQuantity(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "Quantity is required");
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        BigDecimal normalized = quantity.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
        if (normalized.precision() > 12) {
            throw new IllegalArgumentException("Quantity exceeds the supported precision");
        }
        return normalized;
    }

    public static BigDecimal normalizeQuantity(BigDecimal quantity, UnitOfMeasure unit) {
        BigDecimal normalized = normalizeQuantity(quantity);
        Objects.requireNonNull(unit, "Unit of measure is required");
        if ((unit == UnitOfMeasure.UND || unit == UnitOfMeasure.PACK)
                && normalized.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("UND and PACK quantities must be whole numbers");
        }
        return normalized;
    }

    public static BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        BigDecimal normalized = value.setScale(MONEY_SCALE, ROUNDING_MODE);
        if (normalized.precision() > 12) {
            throw new IllegalArgumentException(fieldName + " exceeds the supported precision");
        }
        return normalized;
    }

    public static BigDecimal subtotal(BigDecimal unitPrice, BigDecimal quantity) {
        return normalizeMoney(unitPrice, "Unit price")
                .multiply(normalizeQuantity(quantity))
                .setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal total(List<SaleItem> items) {
        Objects.requireNonNull(items, "Sale items are required");
        return items.stream()
                .map(SaleItem::subtotal)
                .reduce(BigDecimal.ZERO.setScale(MONEY_SCALE), BigDecimal::add)
                .setScale(MONEY_SCALE, ROUNDING_MODE);
    }
}
