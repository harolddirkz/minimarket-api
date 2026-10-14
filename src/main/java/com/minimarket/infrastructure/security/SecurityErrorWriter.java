package com.minimarket.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiError;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String message,
            String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError error = new ApiError(
                code, Instant.now(clock), request.getRequestURI(), MDC.get("correlationId"), null);
        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(status, message, error));
    }
}
