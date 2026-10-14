package com.minimarket.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record SaleItem(
        Long id,
        Long saleId,
        Long productId,
        String productName,
        String productPresentation,
        String productPackagingMaterial,
        BigDecimal productContentQuantity,
        String productContentUnit,
        String productVariant,
        Integer productUnitsPerPackage,
        BigDecimal productUnitContentQuantity,
        String productUnitContentUnit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {

    public SaleItem {
        Objects.requireNonNull(productId, "Product id is required");
        productName = requireText(productName, "Product name is required");
        productPresentation = normalizeOptional(productPresentation);
        productPackagingMaterial = normalizeOptional(productPackagingMaterial);
        productContentUnit = normalizeOptional(productContentUnit);
        if ((productContentQuantity == null) != (productContentUnit == null)) {
            throw new IllegalArgumentException("Product content quantity and unit must be provided together");
        }
        if (productContentQuantity != null) {
            if (productContentQuantity.signum() <= 0) {
                throw new IllegalArgumentException("Product content quantity must be greater than zero");
            }
            productContentQuantity = productContentQuantity.setScale(3);
        }
        productVariant = normalizeOptional(productVariant);
        if (productUnitsPerPackage != null && productUnitsPerPackage <= 0) {
            throw new IllegalArgumentException("Product units per package must be greater than zero");
        }
        productUnitContentUnit = normalizeOptional(productUnitContentUnit);
        if ((productUnitContentQuantity == null) != (productUnitContentUnit == null)) {
            throw new IllegalArgumentException("Product unit content quantity and unit must be provided together");
        }
        if (productUnitContentQuantity != null) {
            productUnitContentQuantity = productUnitContentQuantity.setScale(3);
        }
        quantity = SalePricingPolicy.normalizeQuantity(quantity);
        unitPrice = SalePricingPolicy.normalizeMoney(unitPrice, "Unit price");
        subtotal = SalePricingPolicy.normalizeMoney(subtotal, "Subtotal");
        BigDecimal expected = SalePricingPolicy.subtotal(unitPrice, quantity);
        if (subtotal.compareTo(expected) != 0) {
            throw new IllegalArgumentException("Subtotal does not match unit price and quantity");
        }
    }

    public SaleItem(
            Long id, Long saleId, Long productId, String productName,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this(id, saleId, productId, productName, null, null, null, null, null, null, null, null,
                quantity, unitPrice, subtotal);
    }

    public SaleItem(Long id, Long saleId, Long productId, String productName,
            String productPresentation, BigDecimal productContentQuantity, String productContentUnit,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this(id, saleId, productId, productName, productPresentation, null, productContentQuantity, productContentUnit,
                null, null, null, null, quantity, unitPrice, subtotal);
    }

    public static SaleItem priced(Product product, BigDecimal quantity) {
        Objects.requireNonNull(product, "Product is required");
        BigDecimal normalizedQuantity = SalePricingPolicy.normalizeQuantity(quantity, product.unitOfMeasure());
        BigDecimal unitPrice = SalePricingPolicy.normalizeMoney(product.price(), "Unit price");
        return new SaleItem(null, null, product.id(), product.name(), product.presentation(), product.packagingMaterial(),
                product.contentQuantity(), product.contentUnit(), product.variant(), product.unitsPerPackage(),
                product.unitContentQuantity(), product.unitContentUnit(), normalizedQuantity,
                unitPrice, SalePricingPolicy.subtotal(unitPrice, normalizedQuantity));
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
}
