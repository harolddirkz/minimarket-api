package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.minimarket.domain.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 80, message = "El nombre de usuario debe tener como máximo 80 caracteres")
        @Pattern(regexp = "\\S+", message = "El nombre de usuario no debe contener espacios")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 12, max = 128, message = "La contraseña debe tener entre 12 y 128 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 160, message = "El nombre completo debe tener como máximo 160 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        UserRole role) {
}
