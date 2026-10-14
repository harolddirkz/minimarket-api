package com.minimarket.domain.port.out;

import com.minimarket.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAllActive();

    boolean existsByNameIgnoreCase(String name);
}
