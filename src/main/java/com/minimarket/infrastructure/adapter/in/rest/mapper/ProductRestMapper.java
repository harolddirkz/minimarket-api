package com.minimarket.infrastructure.adapter.in.rest.mapper;

import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.port.in.CreateProductCommand;
import com.minimarket.domain.port.in.UpdateProductCommand;
import com.minimarket.infrastructure.adapter.in.rest.dto.PageResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductCreateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductUpdateRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ProductRestMapper {

    private ProductRestMapper() {
    }

    public static CreateProductCommand toCommand(ProductCreateRequest request) {
        return new CreateProductCommand(
                request.name(), request.description(), request.barcode(), request.price(),
                request.purchasePrice(), request.presentation(), request.packagingMaterial(), request.contentQuantity(),
                request.contentUnit(), request.variant(), request.unitsPerPackage(),
                request.unitContentQuantity(), request.unitContentUnit(), request.categoryId(), request.unitOfMeasure());
    }

    public static UpdateProductCommand toCommand(ProductUpdateRequest request) {
        return new UpdateProductCommand(
                request.name(), request.description(), request.barcode(), request.price(),
                request.purchasePrice(), request.presentation(), request.packagingMaterial(), request.contentQuantity(),
                request.contentUnit(), request.variant(), request.unitsPerPackage(),
                request.unitContentQuantity(), request.unitContentUnit(), request.categoryId(), request.unitOfMeasure());
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(), product.name(), product.description(), product.barcode(), product.price(),
                product.purchasePrice(), grossMarginPercentage(product), markupPercentage(product),
                product.presentation(), product.packagingMaterial(), product.contentQuantity(), product.contentUnit(),
                product.variant(), product.unitsPerPackage(), product.unitContentQuantity(), product.unitContentUnit(),
                product.categoryId(), product.unitOfMeasure(), product.active(),
                product.createdAt(), product.updatedAt());
    }

    private static BigDecimal grossMarginPercentage(Product product) {
        if (product.purchasePrice() == null) {
            return null;
        }
        return product.price().subtract(product.purchasePrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(product.price(), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal markupPercentage(Product product) {
        if (product.purchasePrice() == null) {
            return null;
        }
        return product.price().subtract(product.purchasePrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(product.purchasePrice(), 2, RoundingMode.HALF_UP);
    }

    public static PageResponse<ProductResponse> toResponse(PageResult<Product> page) {
        return new PageResponse<>(
                page.content().stream().map(ProductRestMapper::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
