package com.minimarket.infrastructure.adapter.in.rest.dto;

public record ApiResponse<T>(int statusCode, String message, T data) {
}
