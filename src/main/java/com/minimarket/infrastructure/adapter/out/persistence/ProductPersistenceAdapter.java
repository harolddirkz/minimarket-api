package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.port.out.ProductRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.entity.CategoryEntity;
import com.minimarket.infrastructure.adapter.out.persistence.entity.ProductEntity;
import com.minimarket.infrastructure.adapter.out.persistence.mapper.ProductPersistenceMapper;
import com.minimarket.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import com.minimarket.infrastructure.adapter.out.persistence.repository.ProductJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final ProductJpaRepository productRepository;
    private final CategoryJpaRepository categoryRepository;

    public ProductPersistenceAdapter(
            ProductJpaRepository productRepository,
            CategoryJpaRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product save(Product product) {
        CategoryEntity category = categoryRepository.getReferenceById(product.categoryId());
        return ProductPersistenceMapper.toDomain(
                productRepository.save(ProductPersistenceMapper.toEntity(product, category)));
    }

    @Override
    public Optional<Product> findActiveById(Long id) {
        return productRepository.findByIdAndActiveTrue(id).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Product> findActiveByBarcode(String barcode) {
        return productRepository.findByBarcodeAndActiveTrue(barcode).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public List<Product> findAllById(Collection<Long> ids) {
        return productRepository.findAllByIdIn(ids).stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<Product> findAllActive(PageCriteria criteria) {
        return toPageResult(productRepository.findAllByActiveTrue(toPageable(criteria)));
    }

    @Override
    public PageResult<Product> searchActiveByName(String name, PageCriteria criteria) {
        return toPageResult(productRepository.findAllByActiveTrueAndNameContainingIgnoreCase(
                name.trim(), toPageable(criteria)));
    }

    @Override
    public boolean existsByBarcode(String barcode) {
        return productRepository.existsByBarcode(barcode);
    }

    private static PageRequest toPageable(PageCriteria criteria) {
        return PageRequest.of(criteria.page(), criteria.size(), DEFAULT_SORT);
    }

    private static PageResult<Product> toPageResult(Page<ProductEntity> page) {
        return new PageResult<>(
                page.getContent().stream().map(ProductPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
