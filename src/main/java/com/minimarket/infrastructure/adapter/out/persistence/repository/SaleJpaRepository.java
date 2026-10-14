package com.minimarket.infrastructure.adapter.out.persistence.repository;

import com.minimarket.infrastructure.adapter.out.persistence.entity.SaleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SaleJpaRepository extends JpaRepository<SaleEntity, Long>, JpaSpecificationExecutor<SaleEntity> {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<SaleEntity> findById(Long id);

    @Query(value = "SELECT 'V-' || LPAD(nextval('sale_number_seq')::text, 8, '0')", nativeQuery = true)
    String nextSaleNumber();
}
