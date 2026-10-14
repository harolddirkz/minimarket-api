package com.minimarket.infrastructure.adapter.out.persistence.repository;

import com.minimarket.infrastructure.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByIdAndActiveTrue(Long id);

    Optional<ProductEntity> findByBarcodeAndActiveTrue(String barcode);

    List<ProductEntity> findAllByIdIn(Collection<Long> ids);

    Page<ProductEntity> findAllByActiveTrue(Pageable pageable);

    Page<ProductEntity> findAllByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByBarcode(String barcode);
}
