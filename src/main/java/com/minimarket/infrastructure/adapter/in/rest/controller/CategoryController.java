package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.port.in.CategoryUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.CategoryCreateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.CategoryResponse;
import com.minimarket.infrastructure.adapter.in.rest.mapper.CategoryRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;
    private final ApiResponseFactory responseFactory;

    public CategoryController(CategoryUseCase categoryUseCase, ApiResponseFactory responseFactory) {
        this.categoryUseCase = categoryUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody ApiRequest<CategoryCreateRequest> request) {
        CategoryResponse response = CategoryRestMapper.toResponse(
                categoryUseCase.create(CategoryRestMapper.toCommand(request.data())));
        return responseFactory.created("Categoría registrada correctamente", response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAllActive() {
        List<CategoryResponse> response = categoryUseCase.findAllActive().stream()
                .map(CategoryRestMapper::toResponse)
                .toList();
        return responseFactory.ok("Categorías encontradas correctamente", response);
    }
}
