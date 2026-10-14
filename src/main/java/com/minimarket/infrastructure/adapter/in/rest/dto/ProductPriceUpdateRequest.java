package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductPriceUpdateRequest(
        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal price) {
}
