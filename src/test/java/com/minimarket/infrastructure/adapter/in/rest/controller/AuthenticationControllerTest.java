package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.AuthenticatedUser;
import com.minimarket.domain.port.in.AuthenticationUseCase;
import com.minimarket.domain.port.in.LoginResult;
import com.minimarket.domain.port.in.RefreshSessionResult;
import com.minimarket.domain.port.in.SessionUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.RefreshTokenRequest;
import com.minimarket.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock AuthenticationUseCase authenticationUseCase;
    @Mock SessionUseCase sessionUseCase;

    private MockMvc mockMvc;
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        ApiResponseFactory responseFactory = new ApiResponseFactory();
        controller = new AuthenticationController(authenticationUseCase, sessionUseCase, responseFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(
                        responseFactory, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC)))
                .build();
    }

    @Test
    void shouldRespectLoginContractWithoutExposingPasswordHash() throws Exception {
        when(authenticationUseCase.login(any())).thenReturn(new LoginResult(
                "access-token", "refresh-token", "Bearer", 900,
                new AuthenticatedUser(2L, "vendedor01", "Vendedor Principal", UserRole.VENDEDOR)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":{"username":"vendedor01","password":"StrongPassword1!"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Autenticación exitosa"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.user.role").value("VENDEDOR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));
    }

    @Test
    void shouldReturnGenericUnauthorizedResponseForInvalidCredentials() throws Exception {
        when(authenticationUseCase.login(any())).thenThrow(
                new AuthenticationFailedException("INVALID_CREDENTIALS", "Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":{"username":"unknown","password":"wrong-password"}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.data.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRespectRefreshContract() throws Exception {
        when(sessionUseCase.refresh(any())).thenReturn(
                new RefreshSessionResult("new-access", "new-refresh", "Bearer", 900));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":{"refreshToken":"old-refresh"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Sesión renovada correctamente"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void shouldLogoutAuthenticatedUsersSessionAndReturnNullData() throws Exception {
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .subject("admin01")
                .claim("user_id", 7L)
                .build();

        var response = controller.logout(
                new ApiRequest<>(new RefreshTokenRequest("own-refresh")), jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().message()).isEqualTo("Sesión cerrada correctamente");
        assertThat(response.getBody().data()).isNull();

        verify(sessionUseCase).logout(org.mockito.ArgumentMatchers.argThat(command ->
                command.authenticatedUserId().equals(7L)
                        && command.refreshToken().equals("own-refresh")));
    }
}
