package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.port.in.UserAdministrationUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.PageResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.PasswordResetRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.UserCreateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.UserResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.UserRoleUpdateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.UserStatusUpdateRequest;
import com.minimarket.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserAdministrationUseCase useCase;
    private final ApiResponseFactory responseFactory;

    public UserController(UserAdministrationUseCase useCase, ApiResponseFactory responseFactory) {
        this.useCase = useCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody ApiRequest<UserCreateRequest> request) {
        return responseFactory.created("Usuario registrado correctamente",
                UserRestMapper.toResponse(useCase.create(UserRestMapper.toCommand(request.data()))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return responseFactory.ok("Usuarios encontrados correctamente",
                UserRestMapper.toResponse(useCase.findAll(new PageCriteria(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable @Positive Long id) {
        return responseFactory.ok("Usuario encontrado correctamente",
                UserRestMapper.toResponse(useCase.findById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApiRequest<UserStatusUpdateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        return responseFactory.ok("Estado del usuario actualizado correctamente",
                UserRestMapper.toResponse(useCase.updateStatus(userId(jwt), id, request.data().active())));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApiRequest<UserRoleUpdateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        return responseFactory.ok("Rol del usuario actualizado correctamente",
                UserRestMapper.toResponse(useCase.updateRole(userId(jwt), id, request.data().role())));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApiRequest<PasswordResetRequest> request) {
        useCase.resetPassword(id, request.data().newPassword());
        return responseFactory.ok("Contraseña restablecida correctamente", null);
    }

    private static long userId(Jwt jwt) {
        Number value = jwt.getClaim("user_id");
        return value.longValue();
    }
}
