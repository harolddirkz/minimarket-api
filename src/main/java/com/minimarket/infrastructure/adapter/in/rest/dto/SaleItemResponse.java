package com.minimarket.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
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
}
