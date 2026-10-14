package com.minimarket.infrastructure.adapter.out.persistence.repository;

import com.minimarket.infrastructure.adapter.out.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
