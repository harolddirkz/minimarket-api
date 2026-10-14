package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleRequest(
        @NotEmpty(message = "La venta debe contener al menos un producto")
        @Size(max = 100, message = "La venta no puede contener más de 100 productos")
        List<@Valid SaleItemRequest> items) {
}
