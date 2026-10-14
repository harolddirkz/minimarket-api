package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.UnitOfMeasure;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 160, message = "El nombre del producto debe tener como máximo 160 caracteres")
        String name,

        @Size(max = 500, message = "La descripción debe tener como máximo 500 caracteres")
        String description,

        @Size(min = 1, max = 64, message = "El código de barras debe tener entre 1 y 64 caracteres")
        @Pattern(regexp = "\\S+", message = "El código de barras no debe contener espacios")
        String barcode,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal price,

        @Positive(message = "El precio de compra debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio de compra debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal purchasePrice,

        @Size(max = 30, message = "La presentación debe tener como máximo 30 caracteres")
        String presentation,

        @Size(max = 30, message = "El material del envase debe tener como máximo 30 caracteres")
        String packagingMaterial,

        @Positive(message = "La cantidad de contenido debe ser mayor que cero")
        @Digits(integer = 9, fraction = 3, message = "La cantidad de contenido debe tener como máximo 9 enteros y 3 decimales")
        BigDecimal contentQuantity,

        @Size(max = 10, message = "La unidad de contenido debe tener como máximo 10 caracteres")
        String contentUnit,

        @Size(max = 60, message = "La variante debe tener como máximo 60 caracteres") String variant,
        @Positive(message = "Las unidades por presentación deben ser mayores que cero") Integer unitsPerPackage,
        @Positive(message = "El contenido por unidad debe ser mayor que cero")
        @Digits(integer = 9, fraction = 3, message = "El contenido por unidad debe tener como máximo 9 enteros y 3 decimales") BigDecimal unitContentQuantity,
        @Size(max = 10, message = "La unidad del contenido individual debe tener como máximo 10 caracteres") String unitContentUnit,

        @NotNull(message = "La categoría es obligatoria")
        @Positive(message = "El identificador de categoría debe ser positivo")
        Long categoryId,

        @NotNull(message = "La unidad de medida es obligatoria")
        UnitOfMeasure unitOfMeasure) {

    public ProductCreateRequest(
            String name, String description, String barcode, BigDecimal price,
            BigDecimal purchasePrice, Long categoryId, UnitOfMeasure unitOfMeasure) {
        this(name, description, barcode, price, purchasePrice, null, null, null, null, null, null, null, null, categoryId, unitOfMeasure);
    }

    @AssertTrue(message = "La cantidad y la unidad de contenido deben informarse juntas")
    public boolean isContentDefinitionComplete() {
        boolean hasQuantity = contentQuantity != null;
        boolean hasUnit = contentUnit != null && !contentUnit.isBlank();
        return hasQuantity == hasUnit;
    }

    @AssertTrue(message = "El contenido individual y su unidad deben informarse juntos")
    public boolean isUnitContentDefinitionComplete() {
        return (unitContentQuantity != null) == (unitContentUnit != null && !unitContentUnit.isBlank());
    }
}
