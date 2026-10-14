package com.minimarket.infrastructure.adapter.out.persistence.mapper;

import com.minimarket.domain.model.Product;
import com.minimarket.infrastructure.adapter.out.persistence.entity.CategoryEntity;
import com.minimarket.infrastructure.adapter.out.persistence.entity.ProductEntity;

public final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {
    }

    public static Product toDomain(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBarcode(),
                entity.getPrice(),
                entity.getPurchasePrice(),
                entity.getPresentation(),
                entity.getPackagingMaterial(),
                entity.getContentQuantity(),
                entity.getContentUnit(),
                entity.getVariant(),
                entity.getUnitsPerPackage(),
                entity.getUnitContentQuantity(),
                entity.getUnitContentUnit(),
                entity.getCategory().getId(),
                entity.getUnitOfMeasure(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ProductEntity toEntity(Product product, CategoryEntity category) {
        return new ProductEntity(
                product.id(),
                product.name(),
                product.description(),
                product.barcode(),
                product.price(),
                product.purchasePrice(),
                product.presentation(),
                product.packagingMaterial(),
                product.contentQuantity(),
                product.contentUnit(),
                product.variant(),
                product.unitsPerPackage(),
                product.unitContentQuantity(),
                product.unitContentUnit(),
                category,
                product.unitOfMeasure(),
                product.active(),
                product.createdAt(),
                product.updatedAt());
    }
}
