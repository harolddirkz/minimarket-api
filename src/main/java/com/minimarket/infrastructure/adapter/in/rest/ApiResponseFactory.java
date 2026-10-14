package com.minimarket.infrastructure.adapter.in.rest;

import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    public <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return build(HttpStatus.OK, message, data);
    }

    public <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return build(HttpStatus.CREATED, message, data);
    }

    public <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, T data) {
        return build(status, message, data);
    }

    private static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(status.value(), message, data));
    }
}
