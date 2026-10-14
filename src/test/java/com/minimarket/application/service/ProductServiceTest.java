package com.minimarket.application.service;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.model.Category;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.model.UnitOfMeasure;
import com.minimarket.domain.port.in.CreateProductCommand;
import com.minimarket.domain.port.in.UpdateProductCommand;
import com.minimarket.domain.port.out.CategoryRepositoryPort;
import com.minimarket.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:30:00Z");

    @Mock
    private ProductRepositoryPort productRepository;
    @Mock
    private CategoryRepositoryPort categoryRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(
                productRepository, categoryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCreateValidProduct() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.existsByBarcode("0012345678905")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        Product result = service.create(validCommand());

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.barcode()).isEqualTo("0012345678905");
        assertThat(result.price()).isEqualByComparingTo("4.50");
        assertThat(result.purchasePrice()).isEqualByComparingTo("3.00");
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateProductWithCompletePresentation() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.existsByBarcode("0012345678905")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        CreateProductCommand command = new CreateProductCommand(
                "Leche Gloria", null, "0012345678905", new BigDecimal("4.50"),
                new BigDecimal("3.00"), "  LATA  ", new BigDecimal("400"), " GR ",
                1L, UnitOfMeasure.UND);

        Product result = service.create(command);

        assertThat(result.presentation()).isEqualTo("LATA");
        assertThat(result.contentQuantity()).isEqualByComparingTo("400.000");
        assertThat(result.contentUnit()).isEqualTo("GR");
    }

    @Test
    void shouldCreateProductWithPresentationOnly() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        CreateProductCommand command = new CreateProductCommand(
                "Galletas", null, null, new BigDecimal("1.00"), new BigDecimal("0.60"),
                "PAQUETE", null, null, 1L, UnitOfMeasure.UND);

        Product result = service.create(command);

        assertThat(result.presentation()).isEqualTo("PAQUETE");
        assertThat(result.contentQuantity()).isNull();
        assertThat(result.contentUnit()).isNull();
    }

    @Test
    void shouldCreateProductWithVariantAndPackageDetails() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        CreateProductCommand command = new CreateProductCommand(
                "Cerveza", null, null, new BigDecimal("24.00"), new BigDecimal("18.00"),
                "SIXPACK", null, null, "Lata", 6, new BigDecimal("355"), "ML",
                1L, UnitOfMeasure.UND);

        Product result = service.create(command);

        assertThat(result.variant()).isEqualTo("Lata");
        assertThat(result.unitsPerPackage()).isEqualTo(6);
        assertThat(result.unitContentQuantity()).isEqualByComparingTo("355.000");
        assertThat(result.unitContentUnit()).isEqualTo("ML");
    }

    @Test
    void shouldRejectIncompleteOrInvalidContentDefinition() {
        CreateProductCommand missingUnit = new CreateProductCommand(
                "Leche", null, null, new BigDecimal("4.50"), null,
                null, new BigDecimal("400"), null, 1L, UnitOfMeasure.UND);
        CreateProductCommand missingQuantity = new CreateProductCommand(
                "Leche", null, null, new BigDecimal("4.50"), null,
                null, null, "GR", 1L, UnitOfMeasure.UND);
        CreateProductCommand zero = new CreateProductCommand(
                "Leche", null, null, new BigDecimal("4.50"), null,
                null, BigDecimal.ZERO, "GR", 1L, UnitOfMeasure.UND);
        CreateProductCommand excessiveScale = new CreateProductCommand(
                "Leche", null, null, new BigDecimal("4.50"), null,
                null, new BigDecimal("1.2345"), "LT", 1L, UnitOfMeasure.UND);

        assertThatThrownBy(() -> service.create(missingUnit)).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INCOMPLETE_CONTENT_DEFINITION");
        assertThatThrownBy(() -> service.create(missingQuantity)).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INCOMPLETE_CONTENT_DEFINITION");
        assertThatThrownBy(() -> service.create(zero)).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_CONTENT_QUANTITY");
        assertThatThrownBy(() -> service.create(excessiveScale)).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_CONTENT_QUANTITY_PRECISION");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldUpdateAndExplicitlyClearPresentation() {
        Product presented = new Product(
                10L, "Leche", null, null, new BigDecimal("4.50"), new BigDecimal("3.00"),
                "LATA", new BigDecimal("400"), "GR", 1L, UnitOfMeasure.UND, true, NOW, NOW);
        when(productRepository.findActiveById(10L)).thenReturn(Optional.of(presented));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = service.update(10L, new UpdateProductCommand(
                "Leche", null, null, new BigDecimal("4.50"), new BigDecimal("3.00"),
                null, null, null, 1L, UnitOfMeasure.UND));

        assertThat(result.presentation()).isNull();
        assertThat(result.contentQuantity()).isNull();
        assertThat(result.contentUnit()).isNull();
    }

    @Test
    void shouldRejectInvalidPrice() {
        CreateProductCommand command = new CreateProductCommand(
                "Leche", null, null, BigDecimal.ZERO, null, 1L, UnitOfMeasure.UND);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_PRICE");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidPurchasePrice() {
        CreateProductCommand command = new CreateProductCommand(
                "Leche", null, null, new BigDecimal("4.50"), BigDecimal.ZERO,
                1L, UnitOfMeasure.UND);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_PURCHASE_PRICE");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicatedBarcode() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory()));
        when(productRepository.existsByBarcode("0012345678905")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("BARCODE_ALREADY_EXISTS");
    }

    @Test
    void shouldRejectMissingCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_NOT_FOUND");
    }

    @Test
    void shouldSearchByTrimmedNameWithPagination() {
        PageCriteria criteria = new PageCriteria(1, 10);
        PageResult<Product> expected = new PageResult<>(List.of(product()), 1, 10, 11, 2);
        when(productRepository.searchActiveByName("arroz", criteria)).thenReturn(expected);

        PageResult<Product> result = service.searchActiveByName("  arroz  ", criteria);

        assertThat(result).isSameAs(expected);
        verify(productRepository).searchActiveByName("arroz", criteria);
    }

    @Test
    void shouldFindByExactBarcode() {
        when(productRepository.findActiveByBarcode("0012345678905"))
                .thenReturn(Optional.of(product()));

        Product result = service.findActiveByBarcode("0012345678905");

        assertThat(result.id()).isEqualTo(10L);
        verify(productRepository).findActiveByBarcode("0012345678905");
    }

    @Test
    void shouldRejectMissingProduct() {
        when(productRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findActiveById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void shouldUpdateOnlyPriceAndTimestamp() {
        when(productRepository.findActiveById(10L)).thenReturn(Optional.of(product()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = service.updatePrice(10L, new BigDecimal("5.20"));

        assertThat(result.price()).isEqualByComparingTo("5.20");
        assertThat(result.purchasePrice()).isEqualByComparingTo("3.00");
        assertThat(result.name()).isEqualTo("Leche Gloria");
        assertThat(result.barcode()).isEqualTo("0012345678905");
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldDeactivateProductWithoutDeletingIt() {
        when(productRepository.findActiveById(10L)).thenReturn(Optional.of(product()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

        service.deactivate(10L);

        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().active()).isFalse();
        assertThat(captor.getValue().id()).isEqualTo(10L);
    }

    @Test
    void shouldReturnPagedActiveProducts() {
        PageCriteria criteria = new PageCriteria(0, 20);
        PageResult<Product> expected = new PageResult<>(List.of(product()), 0, 20, 1, 1);
        when(productRepository.findAllActive(criteria)).thenReturn(expected);

        assertThat(service.findAllActive(criteria)).isSameAs(expected);
    }

    private static CreateProductCommand validCommand() {
        return new CreateProductCommand(
                "Leche Gloria", "Leche evaporada", "0012345678905",
                new BigDecimal("4.50"), new BigDecimal("3.00"), 1L, UnitOfMeasure.UND);
    }

    private static Category activeCategory() {
        return new Category(1L, "Lácteos", null, true);
    }

    private static Product product() {
        Instant createdAt = Instant.parse("2026-10-01T10:00:00Z");
        return new Product(
                10L, "Leche Gloria", "Leche evaporada", "0012345678905",
                new BigDecimal("4.50"), new BigDecimal("3.00"), 1L, UnitOfMeasure.UND, true,
                createdAt, createdAt);
    }

    private static Product withId(Product product) {
        return new Product(
                10L, product.name(), product.description(), product.barcode(), product.price(),
                product.purchasePrice(),
                product.presentation(), product.contentQuantity(), product.contentUnit(),
                product.variant(), product.unitsPerPackage(), product.unitContentQuantity(), product.unitContentUnit(),
                product.categoryId(), product.unitOfMeasure(), product.active(),
                product.createdAt(), product.updatedAt());
    }
}
