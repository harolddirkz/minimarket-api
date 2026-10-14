package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SystemInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final ApiResponseFactory responseFactory;

    public SystemController(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<SystemInfoResponse>> info() {
        return responseFactory.ok(
                "Información del sistema obtenida correctamente",
                new SystemInfoResponse("minimarket-api", "UP"));
    }
}
