package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El identificador del producto debe ser positivo")
        Long productId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 9, fraction = 3,
                message = "La cantidad debe tener como máximo 9 enteros y 3 decimales")
        BigDecimal quantity) {
}
