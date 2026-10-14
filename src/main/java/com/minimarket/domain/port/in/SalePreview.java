package com.minimarket.domain.port.in;

import com.minimarket.domain.model.SaleItem;

import java.math.BigDecimal;
import java.util.List;

public record SalePreview(List<SaleItem> items, BigDecimal total) {

    public SalePreview {
        items = List.copyOf(items);
    }
}
