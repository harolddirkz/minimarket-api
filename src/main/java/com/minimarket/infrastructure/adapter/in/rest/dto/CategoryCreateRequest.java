package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre de la categoría debe tener como máximo 100 caracteres")
        String name,

        @Size(max = 500, message = "La descripción debe tener como máximo 500 caracteres")
        String description) {
}
