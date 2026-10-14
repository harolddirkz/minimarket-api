package com.minimarket.infrastructure.adapter.out.persistence.mapper;

import com.minimarket.domain.model.Category;
import com.minimarket.infrastructure.adapter.out.persistence.entity.CategoryEntity;

public final class CategoryPersistenceMapper {

    private CategoryPersistenceMapper() {
    }

    public static Category toDomain(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName(), entity.getDescription(), entity.isActive());
    }

    public static CategoryEntity toEntity(Category category) {
        return new CategoryEntity(category.id(), category.name(), category.description(), category.active());
    }
}
