package com.minimarket.infrastructure.adapter.in.rest.mapper;

import com.minimarket.domain.model.Category;
import com.minimarket.domain.port.in.CreateCategoryCommand;
import com.minimarket.infrastructure.adapter.in.rest.dto.CategoryCreateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.CategoryResponse;

public final class CategoryRestMapper {

    private CategoryRestMapper() {
    }

    public static CreateCategoryCommand toCommand(CategoryCreateRequest request) {
        return new CreateCategoryCommand(request.name(), request.description());
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.id(), category.name(), category.description(), category.active());
    }
}
