package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String barcode,
        BigDecimal price,
        BigDecimal purchasePrice,
        BigDecimal grossMarginPercentage,
        BigDecimal markupPercentage,
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
}
