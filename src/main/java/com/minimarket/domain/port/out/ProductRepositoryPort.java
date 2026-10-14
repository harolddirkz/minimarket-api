package com.minimarket.domain.port.out;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findActiveById(Long id);

    Optional<Product> findActiveByBarcode(String barcode);

    List<Product> findAllById(Collection<Long> ids);

    PageResult<Product> findAllActive(PageCriteria pageCriteria);

    PageResult<Product> searchActiveByName(String name, PageCriteria pageCriteria);

    boolean existsByBarcode(String barcode);
}
