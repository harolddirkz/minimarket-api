package com.minimarket.domain.model;

import java.math.BigDecimal;

public record SaleSummary(long totalSales, BigDecimal totalAmount, String currency) {

    public SaleSummary(long totalSales, BigDecimal totalAmount) {
        this(totalSales, totalAmount, "PEN");
    }
}
