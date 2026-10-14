package com.minimarket.domain.port.out;

import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.SaleFilter;
import com.minimarket.domain.model.SaleListEntry;
import com.minimarket.domain.model.SaleSummary;

import java.util.Optional;

public interface SaleRepositoryPort {

    Sale save(Sale sale);

    Optional<Sale> findById(Long id);

    String nextSaleNumber();

    PageResult<SaleListEntry> findAll(SaleFilter filter, PageCriteria pageCriteria);

    SaleSummary summarize(SaleFilter filter);
}
