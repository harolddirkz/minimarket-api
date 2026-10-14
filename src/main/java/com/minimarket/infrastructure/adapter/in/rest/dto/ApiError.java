package com.minimarket.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        Instant timestamp,
        String path,
        String traceId,
        List<ValidationError> errors) {
}
