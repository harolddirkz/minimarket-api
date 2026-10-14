package com.minimarket.domain.port.in;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;

import java.math.BigDecimal;

public interface ProductUseCase {

    Product create(CreateProductCommand command);

    PageResult<Product> findAllActive(PageCriteria pageCriteria);

    Product findActiveById(Long id);

    PageResult<Product> searchActiveByName(String name, PageCriteria pageCriteria);

    Product findActiveByBarcode(String barcode);

    Product update(Long id, UpdateProductCommand command);

    Product updatePrice(Long id, BigDecimal price);

    void deactivate(Long id);
}
