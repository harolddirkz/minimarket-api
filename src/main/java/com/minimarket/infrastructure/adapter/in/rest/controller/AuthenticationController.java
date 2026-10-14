package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.port.in.AuthenticationUseCase;
import com.minimarket.domain.port.in.LoginCommand;
import com.minimarket.domain.port.in.LoginResult;
import com.minimarket.domain.port.in.LogoutCommand;
import com.minimarket.domain.port.in.RefreshSessionCommand;
import com.minimarket.domain.port.in.RefreshSessionResult;
import com.minimarket.domain.port.in.SessionUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.AuthenticatedUserResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.LoginResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.RefreshSessionResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationUseCase authenticationUseCase;
    private final ApiResponseFactory responseFactory;
    private final SessionUseCase sessionUseCase;

    public AuthenticationController(
            AuthenticationUseCase authenticationUseCase,
            SessionUseCase sessionUseCase,
            ApiResponseFactory responseFactory) {
        this.authenticationUseCase = authenticationUseCase;
        this.sessionUseCase = sessionUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody ApiRequest<LoginRequest> request,
            HttpServletRequest httpRequest) {
        LoginResult result = authenticationUseCase.login(new LoginCommand(
                request.data().username(), request.data().password(), httpRequest.getRemoteAddr()));
        LoginResponse response = new LoginResponse(
                result.accessToken(), result.refreshToken(), result.tokenType(), result.expiresIn(),
                new AuthenticatedUserResponse(
                        result.user().id(), result.user().username(), result.user().fullName(), result.user().role()));
        return responseFactory.ok("Autenticación exitosa", response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshSessionResponse>> refresh(
            @Valid @RequestBody ApiRequest<RefreshTokenRequest> request) {
        RefreshSessionResult result = sessionUseCase.refresh(
                new RefreshSessionCommand(request.data().refreshToken()));
        return responseFactory.ok("Sesión renovada correctamente", new RefreshSessionResponse(
                result.accessToken(), result.refreshToken(), result.tokenType(), result.expiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody ApiRequest<RefreshTokenRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        Number userId = jwt.getClaim("user_id");
        sessionUseCase.logout(new LogoutCommand(userId.longValue(), request.data().refreshToken()));
        return responseFactory.ok("Sesión cerrada correctamente", null);
    }
}
