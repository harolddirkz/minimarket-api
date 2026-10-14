package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.model.User;
import com.minimarket.domain.port.in.UserAdministrationUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.AuthenticatedUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticatedUserController {

    private final UserAdministrationUseCase useCase;
    private final ApiResponseFactory responseFactory;

    public AuthenticatedUserController(UserAdministrationUseCase useCase, ApiResponseFactory responseFactory) {
        this.useCase = useCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthenticatedUserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        Number userId = jwt.getClaim("user_id");
        User user = useCase.findAuthenticatedUser(userId.longValue());
        return responseFactory.ok("Usuario autenticado obtenido correctamente",
                new AuthenticatedUserResponse(user.id(), user.username(), user.fullName(), user.role()));
    }
}
