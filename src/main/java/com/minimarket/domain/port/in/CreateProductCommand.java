package com.minimarket.domain.port.in;

import com.minimarket.domain.model.UnitOfMeasure;

import java.math.BigDecimal;

public record CreateProductCommand(
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
        UnitOfMeasure unitOfMeasure) {

    public CreateProductCommand(
            String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, String presentation, BigDecimal contentQuantity,
            String contentUnit, Long categoryId, UnitOfMeasure unitOfMeasure) {
        this(name, description, barcode, price, purchasePrice, presentation, null, contentQuantity, contentUnit,
                null, null, null, null, categoryId, unitOfMeasure);
    }

    public CreateProductCommand(String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, String presentation, BigDecimal contentQuantity, String contentUnit,
            String variant, Integer unitsPerPackage, BigDecimal unitContentQuantity, String unitContentUnit,
            Long categoryId, UnitOfMeasure unitOfMeasure) {
        this(name, description, barcode, price, purchasePrice, presentation, null, contentQuantity, contentUnit,
                variant, unitsPerPackage, unitContentQuantity, unitContentUnit, categoryId, unitOfMeasure);
    }

    public CreateProductCommand(
            String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, Long categoryId, UnitOfMeasure unitOfMeasure) {
        this(name, description, barcode, price, purchasePrice, null, null, null, null,
                null, null, null, null, categoryId, unitOfMeasure);
    }
}
