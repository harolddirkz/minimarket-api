package com.minimarket.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ApiRequest<T>(@NotNull @Valid T data) {
}
