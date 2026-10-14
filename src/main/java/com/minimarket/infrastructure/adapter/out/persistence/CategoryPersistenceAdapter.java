package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.Category;
import com.minimarket.domain.port.out.CategoryRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.mapper.CategoryPersistenceMapper;
import com.minimarket.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository repository;

    public CategoryPersistenceAdapter(CategoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Category save(Category category) {
        return CategoryPersistenceMapper.toDomain(repository.save(CategoryPersistenceMapper.toEntity(category)));
    }

    @Override
    public Optional<Category> findById(Long id) {
        return repository.findById(id).map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public List<Category> findAllActive() {
        return repository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(CategoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }
}
