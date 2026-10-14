package com.minimarket.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalePricingPolicyTest {

    @Test
    void shouldCalculateAndRoundWeightedProductSubtotal() {
        Product product = product(UnitOfMeasure.KG, "4.55");

        SaleItem item = SaleItem.priced(product, new BigDecimal("0.555"));

        assertThat(item.quantity()).isEqualByComparingTo("0.555");
        assertThat(item.subtotal()).isEqualByComparingTo("2.53");
    }

    @Test
    void shouldRequireWholeQuantitiesForUnitsAndPacks() {
        assertThatThrownBy(() -> SaleItem.priced(product(UnitOfMeasure.UND, "4.50"),
                new BigDecimal("1.500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whole numbers");
    }

    @Test
    void shouldRejectInvalidQuantityAndUnsupportedPrecision() {
        assertThatThrownBy(() -> SalePricingPolicy.normalizeQuantity(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SalePricingPolicy.normalizeQuantity(new BigDecimal("0.0001")))
                .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> SalePricingPolicy.normalizeQuantity(new BigDecimal("1000000000.000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precision");
    }

    @Test
    void shouldRejectDuplicateProductsInSale() {
        SaleItem first = SaleItem.priced(product(UnitOfMeasure.KG, "4.00"), BigDecimal.ONE);
        SaleItem duplicate = SaleItem.priced(product(UnitOfMeasure.KG, "4.00"), BigDecimal.ONE);

        assertThatThrownBy(() -> new Sale(null, "V-00000001", 1L, SaleStatus.COMPLETED,
                new BigDecimal("8.00"), List.of(first, duplicate), Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate products");
    }

    private static Product product(UnitOfMeasure unit, String price) {
        Instant now = Instant.parse("2026-10-08T15:00:00Z");
        return new Product(1L, "Producto", null, null, new BigDecimal(price), null,
                1L, unit, true, now, now);
    }
}
