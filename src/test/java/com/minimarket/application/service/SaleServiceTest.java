package com.minimarket.application.service;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.exception.ForbiddenOperationException;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.SaleIdempotency;
import com.minimarket.domain.model.SaleIdempotencyStatus;
import com.minimarket.domain.model.SaleStatus;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.SaleSummary;
import com.minimarket.domain.model.UnitOfMeasure;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.PreviewSaleCommand;
import com.minimarket.domain.port.in.RegisterSaleCommand;
import com.minimarket.domain.port.in.SaleLineCommand;
import com.minimarket.domain.port.in.SaleHistoryQuery;
import com.minimarket.domain.port.in.SaleSummaryQuery;
import com.minimarket.domain.port.in.SalePreview;
import com.minimarket.domain.port.out.ProductRepositoryPort;
import com.minimarket.domain.port.out.SaleIdempotencyRepositoryPort;
import com.minimarket.domain.port.out.SaleRepositoryPort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");

    @Mock private ProductRepositoryPort productRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private SaleRepositoryPort saleRepository;
    @Mock private SaleIdempotencyRepositoryPort idempotencyRepository;

    private SaleService service;

    @BeforeEach
    void setUp() {
        service = new SaleService(productRepository, userRepository, saleRepository,
                idempotencyRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldPreviewSeveralProductsUsingBackendPrices() {
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(
                product(1L, "Leche", "4.50", UnitOfMeasure.UND, true),
                product(2L, "Tomate", "4.00", UnitOfMeasure.KG, true)));

        SalePreview result = service.preview(new PreviewSaleCommand(List.of(
                new SaleLineCommand(1L, new BigDecimal("2")),
                new SaleLineCommand(2L, new BigDecimal("0.500")))));

        assertThat(result.items()).extracting(item -> item.subtotal().toPlainString())
                .containsExactly("9.00", "2.00");
        assertThat(result.total()).isEqualByComparingTo("11.00");
        verify(productRepository).findAllById(anyCollection());
    }

    @Test
    void shouldIncludeProductPresentationSnapshotInPreview() {
        Product presented = new Product(
                1L, "Leche", null, null, new BigDecimal("4.50"), null,
                "LATA", new BigDecimal("400"), "GR", 1L, UnitOfMeasure.UND, true, NOW, NOW);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(presented));

        SalePreview result = service.preview(commandFor(1L, "1"));

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.productPresentation()).isEqualTo("LATA");
            assertThat(item.productContentQuantity()).isEqualByComparingTo("400.000");
            assertThat(item.productContentUnit()).isEqualTo("GR");
        });
    }

    @Test
    void shouldRejectEmptyAndDuplicateSalesBeforeQueryingProducts() {
        assertThatThrownBy(() -> service.preview(new PreviewSaleCommand(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.preview(new PreviewSaleCommand(List.of(
                new SaleLineCommand(1L, BigDecimal.ONE),
                new SaleLineCommand(1L, BigDecimal.TWO)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicados");
        verify(productRepository, never()).findAllById(anyCollection());
    }

    @Test
    void shouldDistinguishMissingAndInactiveProducts() {
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of());
        assertThatThrownBy(() -> service.preview(commandFor(99L, "1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("PRODUCT_NOT_FOUND");

        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(
                product(2L, "Inactivo", "2.00", UnitOfMeasure.UND, false)));
        assertThatThrownBy(() -> service.preview(commandFor(2L, "1")))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("PRODUCT_INACTIVE");
    }

    @Test
    void shouldRejectFractionalUnitQuantity() {
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(
                product(1L, "Leche", "4.50", UnitOfMeasure.UND, true)));

        assertThatThrownBy(() -> service.preview(commandFor(1L, "1.500")))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_PRODUCT_QUANTITY");
    }

    @Test
    void shouldRegisterCompletedSaleAndCompleteIdempotencyOperation() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(idempotencyRepository.tryStart(eq(7L), eq("operation-1"), anyString(), eq(NOW)))
                .thenReturn(true);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(
                product(1L, "Leche", "4.50", UnitOfMeasure.UND, true)));
        when(saleRepository.nextSaleNumber()).thenReturn("V-00000001");
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));

        Sale result = service.register(new RegisterSaleCommand(
                7L, " operation-1 ", List.of(new SaleLineCommand(1L, new BigDecimal("2")))));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(result.total()).isEqualByComparingTo("9.00");
        verify(idempotencyRepository).complete(7L, "operation-1", 10L, NOW);
    }

    @Test
    void shouldReturnOriginalSaleForEquivalentRetry() {
        Sale existing = existingSale();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(idempotencyRepository.tryStart(eq(7L), eq("operation-1"), anyString(), eq(NOW)))
                .thenReturn(false);
        when(idempotencyRepository.find(7L, "operation-1")).thenAnswer(invocation -> Optional.of(
                new SaleIdempotency(7L, "operation-1", sha256("1:2.000"),
                        SaleIdempotencyStatus.COMPLETED, 10L, NOW, NOW)));
        when(saleRepository.findById(10L)).thenReturn(Optional.of(existing));

        Sale result = service.register(registerCommand());

        assertThat(result).isSameAs(existing);
        verify(productRepository, never()).findAllById(anyCollection());
        verify(saleRepository, never()).save(any());
    }

    @Test
    void shouldRejectIdempotencyKeyUsedForDifferentRequest() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(idempotencyRepository.tryStart(eq(7L), eq("operation-1"), anyString(), eq(NOW)))
                .thenReturn(false);
        when(idempotencyRepository.find(7L, "operation-1")).thenReturn(Optional.of(
                new SaleIdempotency(7L, "operation-1", "a".repeat(64),
                        SaleIdempotencyStatus.COMPLETED, 10L, NOW, NOW)));

        assertThatThrownBy(() -> service.register(registerCommand()))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void shouldRestrictVendorToOwnSaleAndAllowAdmin() {
        Sale existing = existingSale();
        when(saleRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThat(service.findAccessibleById(10L, 7L, UserRole.VENDEDOR)).isSameAs(existing);
        assertThat(service.findAccessibleById(10L, 99L, UserRole.ADMIN)).isSameAs(existing);
        assertThatThrownBy(() -> service.findAccessibleById(10L, 8L, UserRole.VENDEDOR))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("code").isEqualTo("SALE_ACCESS_DENIED");
    }

    @Test
    void shouldForceVendorHistoryToOwnUser() {
        var expected = new PageResult<com.minimarket.domain.model.SaleListEntry>(List.of(), 0, 20, 0, 0);
        when(saleRepository.findAll(any(), any())).thenReturn(expected);
        var filter = ArgumentCaptor.forClass(com.minimarket.domain.model.SaleFilter.class);

        var result = service.findHistory(new SaleHistoryQuery(
                null, null, null, null, new PageCriteria(0, 20), 7L, UserRole.VENDEDOR));

        assertThat(result).isSameAs(expected);
        verify(saleRepository).findAll(filter.capture(), eq(new PageCriteria(0, 20)));
        assertThat(filter.getValue().userId()).isEqualTo(7L);
    }

    @Test
    void shouldRejectAnotherUsersHistoryAndVendorSummary() {
        assertThatThrownBy(() -> service.findHistory(new SaleHistoryQuery(
                null, null, 8L, null, new PageCriteria(0, 20), 7L, UserRole.VENDEDOR)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.summarize(new SaleSummaryQuery(
                null, null, 7L, UserRole.VENDEDOR)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(saleRepository, never()).summarize(any());
    }

    @Test
    void shouldCalculateAdminSummaryForCompletedSales() {
        when(saleRepository.summarize(any())).thenReturn(new SaleSummary(3, new BigDecimal("27.50")));
        var filter = ArgumentCaptor.forClass(com.minimarket.domain.model.SaleFilter.class);

        SaleSummary result = service.summarize(new SaleSummaryQuery(null, null, 1L, UserRole.ADMIN));

        assertThat(result.totalSales()).isEqualTo(3);
        assertThat(result.totalAmount()).isEqualByComparingTo("27.50");
        verify(saleRepository).summarize(filter.capture());
        assertThat(filter.getValue().status()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(filter.getValue().userId()).isNull();
    }

    private static PreviewSaleCommand commandFor(Long productId, String quantity) {
        return new PreviewSaleCommand(List.of(new SaleLineCommand(productId, new BigDecimal(quantity))));
    }

    private static RegisterSaleCommand registerCommand() {
        return new RegisterSaleCommand(7L, "operation-1",
                List.of(new SaleLineCommand(1L, new BigDecimal("2"))));
    }

    private static User user() {
        return new User(7L, "seller", "hash", "Seller", UserRole.VENDEDOR, true, NOW, NOW);
    }

    private static Product product(Long id, String name, String price, UnitOfMeasure unit, boolean active) {
        return new Product(id, name, null, null, new BigDecimal(price), null,
                1L, unit, active, NOW, NOW);
    }

    private static Sale existingSale() {
        var item = com.minimarket.domain.model.SaleItem.priced(
                product(1L, "Leche", "4.50", UnitOfMeasure.UND, true), new BigDecimal("2"));
        return new Sale(10L, "V-00000001", 7L, SaleStatus.COMPLETED,
                new BigDecimal("9.00"), List.of(item), NOW, NOW);
    }

    private static Sale withId(Sale sale, Long id) {
        return new Sale(id, sale.saleNumber(), sale.userId(), sale.status(), sale.total(), sale.items(),
                sale.createdAt(), sale.updatedAt());
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
