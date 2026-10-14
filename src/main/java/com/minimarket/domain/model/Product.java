package com.minimarket.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public record Product(
        Long id,
        String name,
        String description,
        String barcode,
        BigDecimal price,
        BigDecimal purchasePrice,
        String presentation,
        String packagingMaterial,
        BigDecimal contentQuantity,
        String contentUnit,
        String variant,
        Integer unitsPerPackage,
        BigDecimal unitContentQuantity,
        String unitContentUnit,
        Long categoryId,
        UnitOfMeasure unitOfMeasure,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public Product {
        name = requireText(name, "Product name is required");
        description = normalizeOptional(description);
        barcode = validateOptionalBarcode(barcode);
        price = normalizePrice(price);
        purchasePrice = normalizeOptionalPurchasePrice(purchasePrice);
        presentation = normalizeOptionalText(presentation, 30, "Presentation");
        packagingMaterial = normalizeOptionalText(packagingMaterial, 30, "Packaging material");
        contentUnit = normalizeOptionalText(contentUnit, 10, "Content unit");
        contentQuantity = normalizeOptionalContentQuantity(contentQuantity);
        if ((contentQuantity == null) != (contentUnit == null)) {
            throw new IllegalArgumentException("Content quantity and content unit must be provided together");
        }
        variant = normalizeOptionalText(variant, 60, "Variant");
        if (unitsPerPackage != null && unitsPerPackage <= 0) {
            throw new IllegalArgumentException("Units per package must be greater than zero");
        }
        unitContentUnit = normalizeOptionalText(unitContentUnit, 10, "Unit content unit");
        unitContentQuantity = normalizeOptionalContentQuantity(unitContentQuantity);
        if ((unitContentQuantity == null) != (unitContentUnit == null)) {
            throw new IllegalArgumentException("Unit content quantity and unit must be provided together");
        }
        Objects.requireNonNull(categoryId, "Category id is required");
        Objects.requireNonNull(unitOfMeasure, "Unit of measure is required");
    }

    public Product(
            Long id, String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, Long categoryId, UnitOfMeasure unitOfMeasure,
            boolean active, Instant createdAt, Instant updatedAt) {
        this(id, name, description, barcode, price, purchasePrice, null, null, null, null,
                null, null, null, null,
                categoryId, unitOfMeasure, active, createdAt, updatedAt);
    }

    public Product(
            Long id, String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, String presentation, BigDecimal contentQuantity, String contentUnit,
            Long categoryId, UnitOfMeasure unitOfMeasure, boolean active, Instant createdAt, Instant updatedAt) {
        this(id, name, description, barcode, price, purchasePrice, presentation, null, contentQuantity, contentUnit,
                null, null, null, null, categoryId, unitOfMeasure, active, createdAt, updatedAt);
    }

    public Product(Long id, String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, String presentation, BigDecimal contentQuantity, String contentUnit,
            String variant, Integer unitsPerPackage, BigDecimal unitContentQuantity, String unitContentUnit,
            Long categoryId, UnitOfMeasure unitOfMeasure, boolean active, Instant createdAt, Instant updatedAt) {
        this(id, name, description, barcode, price, purchasePrice, presentation, null, contentQuantity, contentUnit,
                variant, unitsPerPackage, unitContentQuantity, unitContentUnit,
                categoryId, unitOfMeasure, active, createdAt, updatedAt);
    }

    private static BigDecimal normalizeOptionalContentQuantity(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Content quantity must be greater than zero");
        }
        BigDecimal normalized = value.setScale(3, RoundingMode.UNNECESSARY);
        if (normalized.precision() - normalized.scale() > 9) {
            throw new IllegalArgumentException("Content quantity must have at most 9 integer digits");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, int maximumLength, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized != null && normalized.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " exceeds maximum length");
        }
        return normalized;
    }

    private static BigDecimal normalizeOptionalPurchasePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Purchase price must be greater than zero");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal normalizePrice(BigDecimal value) {
        Objects.requireNonNull(value, "Price is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String validateOptionalBarcode(String value) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("Barcode must not be blank");
        }
        return value;
    }
}
