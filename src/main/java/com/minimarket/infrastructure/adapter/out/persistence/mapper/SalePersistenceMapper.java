package com.minimarket.infrastructure.adapter.out.persistence.mapper;

import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.SaleItem;
import com.minimarket.infrastructure.adapter.out.persistence.entity.SaleEntity;
import com.minimarket.infrastructure.adapter.out.persistence.entity.SaleItemEntity;

public final class SalePersistenceMapper {

    private SalePersistenceMapper() {
    }

    public static SaleEntity toEntity(Sale sale) {
        SaleEntity entity = new SaleEntity(sale.id(), sale.saleNumber(), sale.userId(), sale.status(),
                sale.total(), sale.createdAt(), sale.updatedAt());
        sale.items().stream().map(SalePersistenceMapper::toItemEntity).forEach(entity::addItem);
        return entity;
    }

    public static Sale toDomain(SaleEntity entity) {
        return new Sale(
                entity.getId(),
                entity.getSaleNumber(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getTotal(),
                entity.getItems().stream().map(SalePersistenceMapper::toItemDomain).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private static SaleItemEntity toItemEntity(SaleItem item) {
        return new SaleItemEntity(item.id(), item.productId(), item.productName(),
                item.productPresentation(), item.productPackagingMaterial(), item.productContentQuantity(), item.productContentUnit(), item.quantity(),
                item.productVariant(), item.productUnitsPerPackage(),
                item.productUnitContentQuantity(), item.productUnitContentUnit(),
                item.unitPrice(), item.subtotal());
    }

    private static SaleItem toItemDomain(SaleItemEntity entity) {
        return new SaleItem(
                entity.getId(),
                entity.getSale().getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getProductPresentation(),
                entity.getProductPackagingMaterial(),
                entity.getProductContentQuantity(),
                entity.getProductContentUnit(),
                entity.getProductVariant(),
                entity.getProductUnitsPerPackage(),
                entity.getProductUnitContentQuantity(),
                entity.getProductUnitContentUnit(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getSubtotal());
    }
}
