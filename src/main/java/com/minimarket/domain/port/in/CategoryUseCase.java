package com.minimarket.domain.port.in;

import com.minimarket.domain.model.Category;

import java.util.List;

public interface CategoryUseCase {

    Category create(CreateCategoryCommand command);

    List<Category> findAllActive();
}
