package com.minimarket.application.service;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.model.Category;
import com.minimarket.domain.port.in.CategoryUseCase;
import com.minimarket.domain.port.in.CreateCategoryCommand;
import com.minimarket.domain.port.out.CategoryRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService implements CategoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepositoryPort categoryRepository;

    public CategoryService(CategoryRepositoryPort categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Category create(CreateCategoryCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()) {
            throw new BusinessException("INVALID_CATEGORY_NAME", "El nombre de la categoría es obligatorio");
        }

        String normalizedName = command.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría con ese nombre");
        }

        Category saved = categoryRepository.save(new Category(null, normalizedName, command.description(), true));
        log.info("Category created. categoryId={}", saved.id());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findAllActive();
    }
}
