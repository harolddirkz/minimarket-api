package com.minimarket.infrastructure.adapter.in.rest.mapper;

import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.SaleItem;
import com.minimarket.domain.port.in.PreviewSaleCommand;
import com.minimarket.domain.port.in.RegisterSaleCommand;
import com.minimarket.domain.port.in.SaleLineCommand;
import com.minimarket.domain.port.in.SalePreview;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleItemRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleItemResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SalePreviewResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleResponse;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.SaleListEntry;
import com.minimarket.domain.model.SaleSummary;
import com.minimarket.infrastructure.adapter.in.rest.dto.PageResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleListResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleSummaryResponse;

public final class SaleRestMapper {

    private SaleRestMapper() {
    }

    public static PreviewSaleCommand toPreviewCommand(SaleRequest request) {
        return new PreviewSaleCommand(request.items().stream().map(SaleRestMapper::toLineCommand).toList());
    }

    public static RegisterSaleCommand toRegisterCommand(
            SaleRequest request, Long userId, String idempotencyKey) {
        return new RegisterSaleCommand(userId, idempotencyKey,
                request.items().stream().map(SaleRestMapper::toLineCommand).toList());
    }

    public static SalePreviewResponse toResponse(SalePreview preview) {
        return new SalePreviewResponse(
                preview.items().stream().map(SaleRestMapper::toItemResponse).toList(),
                preview.total());
    }

    public static SaleResponse toResponse(Sale sale) {
        return new SaleResponse(
                sale.id(), sale.saleNumber(), sale.userId(), sale.status(), sale.total(),
                sale.items().stream().map(SaleRestMapper::toItemResponse).toList(),
                sale.createdAt(), sale.updatedAt());
    }

    public static PageResponse<SaleListResponse> toResponse(PageResult<SaleListEntry> page) {
        return new PageResponse<>(
                page.content().stream().map(SaleRestMapper::toListResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public static SaleSummaryResponse toResponse(SaleSummary summary) {
        return new SaleSummaryResponse(summary.totalSales(), summary.totalAmount(), summary.currency());
    }

    private static SaleLineCommand toLineCommand(SaleItemRequest request) {
        return new SaleLineCommand(request.productId(), request.quantity());
    }

    private static SaleItemResponse toItemResponse(SaleItem item) {
        return new SaleItemResponse(
                item.productId(), item.productName(), item.productPresentation(), item.productPackagingMaterial(),
                item.productContentQuantity(), item.productContentUnit(),
                item.productVariant(), item.productUnitsPerPackage(),
                item.productUnitContentQuantity(), item.productUnitContentUnit(),
                item.quantity(), item.unitPrice(), item.subtotal());
    }

    private static SaleListResponse toListResponse(SaleListEntry sale) {
        return new SaleListResponse(sale.id(), sale.saleNumber(), sale.userId(), sale.status(),
                sale.total(), sale.createdAt());
    }
}
