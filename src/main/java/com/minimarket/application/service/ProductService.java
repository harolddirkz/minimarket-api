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
import com.minimarket.domain.port.in.ProductUseCase;
import com.minimarket.domain.port.in.UpdateProductCommand;
import com.minimarket.domain.port.out.CategoryRepositoryPort;
import com.minimarket.domain.port.out.ProductRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class ProductService implements ProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepositoryPort productRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final Clock clock;

    public ProductService(
            ProductRepositoryPort productRepository,
            CategoryRepositoryPort categoryRepository,
            Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Product create(CreateProductCommand command) {
        validateCreateCommand(command);
        validateCategoryIsActive(command.categoryId());
        validateBarcodeAvailability(command.barcode(), null);

        Instant now = clock.instant();
        Product product = new Product(
                null,
                command.name(),
                command.description(),
                command.barcode(),
                command.price(),
                command.purchasePrice(),
                command.presentation(),
                command.packagingMaterial(),
                command.contentQuantity(),
                command.contentUnit(),
                command.variant(),
                command.unitsPerPackage(),
                command.unitContentQuantity(),
                command.unitContentUnit(),
                command.categoryId(),
                command.unitOfMeasure(),
                true,
                now,
                now);

        Product saved = productRepository.save(product);
        log.info("Product created. productId={}", saved.id());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Product> findAllActive(PageCriteria pageCriteria) {
        return productRepository.findAllActive(requirePageCriteria(pageCriteria));
    }

    @Override
    @Transactional(readOnly = true)
    public Product findActiveById(Long id) {
        return getActiveProduct(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Product> searchActiveByName(String name, PageCriteria pageCriteria) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("INVALID_SEARCH_NAME", "El nombre de búsqueda es obligatorio");
        }
        String criterion = name.trim();
        log.debug("Searching active products by name. criterion={}", criterion);
        return productRepository.searchActiveByName(criterion, requirePageCriteria(pageCriteria));
    }

    @Override
    @Transactional(readOnly = true)
    public Product findActiveByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new BusinessException("INVALID_BARCODE", "El código de barras es obligatorio");
        }
        log.debug("Searching active product by barcode");
        return productRepository.findActiveByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PRODUCT_NOT_FOUND", "Producto no encontrado"));
    }

    @Override
    @Transactional
    public Product update(Long id, UpdateProductCommand command) {
        validateUpdateCommand(command);
        Product current = getActiveProduct(id);
        validateCategoryIsActive(command.categoryId());
        validateBarcodeAvailability(command.barcode(), current.barcode());

        Product updated = new Product(
                current.id(),
                command.name(),
                command.description(),
                command.barcode(),
                command.price(),
                command.purchasePrice(),
                command.presentation(),
                command.packagingMaterial(),
                command.contentQuantity(),
                command.contentUnit(),
                command.variant(),
                command.unitsPerPackage(),
                command.unitContentQuantity(),
                command.unitContentUnit(),
                command.categoryId(),
                command.unitOfMeasure(),
                true,
                current.createdAt(),
                clock.instant());

        Product saved = productRepository.save(updated);
        log.info("Product updated. productId={}", saved.id());
        return saved;
    }

    @Override
    @Transactional
    public Product updatePrice(Long id, BigDecimal price) {
        validatePrice(price);
        Product current = getActiveProduct(id);
        Product updated = new Product(
                current.id(), current.name(), current.description(), current.barcode(), price,
                current.purchasePrice(),
                current.presentation(), current.packagingMaterial(), current.contentQuantity(), current.contentUnit(),
                current.variant(), current.unitsPerPackage(), current.unitContentQuantity(), current.unitContentUnit(),
                current.categoryId(), current.unitOfMeasure(), true,
                current.createdAt(), clock.instant());

        Product saved = productRepository.save(updated);
        log.info("Product price updated. productId={}", saved.id());
        return saved;
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Product current = getActiveProduct(id);
        Product deactivated = new Product(
                current.id(), current.name(), current.description(), current.barcode(), current.price(),
                current.purchasePrice(),
                current.presentation(), current.packagingMaterial(), current.contentQuantity(), current.contentUnit(),
                current.variant(), current.unitsPerPackage(), current.unitContentQuantity(), current.unitContentUnit(),
                current.categoryId(), current.unitOfMeasure(), false,
                current.createdAt(), clock.instant());
        productRepository.save(deactivated);
        log.info("Product deactivated. productId={}", id);
    }

    private Product getActiveProduct(Long id) {
        if (id == null) {
            throw new BusinessException("INVALID_PRODUCT_ID", "El identificador del producto es obligatorio");
        }
        return productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PRODUCT_NOT_FOUND", "Producto no encontrado"));
    }

    private void validateCategoryIsActive(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("INVALID_CATEGORY_ID", "La categoría es obligatoria");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CATEGORY_NOT_FOUND", "Categoría no encontrada"));
        if (!category.active()) {
            throw new BusinessException("CATEGORY_INACTIVE", "La categoría seleccionada está inactiva");
        }
    }

    private void validateBarcodeAvailability(String barcode, String currentBarcode) {
        validateOptionalBarcode(barcode);
        boolean barcodeChanged = barcode != null && !barcode.equals(currentBarcode);
        if (barcodeChanged && productRepository.existsByBarcode(barcode)) {
            throw new ConflictException("BARCODE_ALREADY_EXISTS", "Ya existe un producto con ese código de barras");
        }
    }

    private static void validateCreateCommand(CreateProductCommand command) {
        if (command == null) {
            throw new BusinessException("INVALID_PRODUCT", "Los datos del producto son obligatorios");
        }
        validateProductFields(command.name(), command.barcode(), command.price(), command.purchasePrice(),
                command.presentation(), command.packagingMaterial(), command.contentQuantity(), command.contentUnit(), command.variant(),
                command.unitsPerPackage(), command.unitContentQuantity(), command.unitContentUnit(), command.unitOfMeasure());
    }

    private static void validateUpdateCommand(UpdateProductCommand command) {
        if (command == null) {
            throw new BusinessException("INVALID_PRODUCT", "Los datos del producto son obligatorios");
        }
        validateProductFields(command.name(), command.barcode(), command.price(), command.purchasePrice(),
                command.presentation(), command.packagingMaterial(), command.contentQuantity(), command.contentUnit(), command.variant(),
                command.unitsPerPackage(), command.unitContentQuantity(), command.unitContentUnit(), command.unitOfMeasure());
    }

    private static void validateProductFields(
            String name, String barcode, BigDecimal price, BigDecimal purchasePrice,
            String presentation, String packagingMaterial, BigDecimal contentQuantity, String contentUnit, String variant,
            Integer unitsPerPackage, BigDecimal unitContentQuantity, String unitContentUnit,
            UnitOfMeasure unitOfMeasure) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("INVALID_PRODUCT_NAME", "El nombre del producto es obligatorio");
        }
        validateOptionalBarcode(barcode);
        validatePrice(price);
        validateOptionalPurchasePrice(purchasePrice);
        validatePresentation(presentation, contentQuantity, contentUnit);
        if (packagingMaterial != null && !packagingMaterial.isBlank() && packagingMaterial.trim().length() > 30) {
            throw new BusinessException("INVALID_PACKAGING_MATERIAL", "El material del envase debe tener como máximo 30 caracteres");
        }
        validatePackageDetails(variant, unitsPerPackage, unitContentQuantity, unitContentUnit);
        if (unitOfMeasure == null) {
            throw new BusinessException("INVALID_UNIT_OF_MEASURE", "La unidad de medida es obligatoria");
        }
    }

    private static void validatePackageDetails(
            String variant, Integer unitsPerPackage, BigDecimal unitContentQuantity, String unitContentUnit) {
        if (variant != null && !variant.isBlank() && variant.trim().length() > 60) {
            throw new BusinessException("INVALID_VARIANT", "La variante debe tener como máximo 60 caracteres");
        }
        if (unitsPerPackage != null && unitsPerPackage <= 0) {
            throw new BusinessException("INVALID_UNITS_PER_PACKAGE", "Las unidades por presentación deben ser mayores que cero");
        }
        String normalizedUnit = unitContentUnit == null || unitContentUnit.isBlank() ? null : unitContentUnit.trim();
        if (normalizedUnit != null && normalizedUnit.length() > 10) {
            throw new BusinessException("INVALID_UNIT_CONTENT_UNIT", "La unidad del contenido individual debe tener como máximo 10 caracteres");
        }
        if ((unitContentQuantity == null) != (normalizedUnit == null)) {
            throw new BusinessException("INCOMPLETE_UNIT_CONTENT", "El contenido individual y su unidad deben informarse juntos");
        }
        if (unitContentQuantity != null && (unitContentQuantity.signum() <= 0
                || Math.max(unitContentQuantity.stripTrailingZeros().scale(), 0) > 3
                || unitContentQuantity.precision() - unitContentQuantity.scale() > 9)) {
            throw new BusinessException("INVALID_UNIT_CONTENT_QUANTITY", "El contenido individual debe ser positivo y tener máximo 9 enteros y 3 decimales");
        }
    }

    private static void validatePresentation(
            String presentation, BigDecimal contentQuantity, String contentUnit) {
        if (presentation != null && !presentation.isBlank() && presentation.trim().length() > 30) {
            throw new BusinessException("INVALID_PRESENTATION", "La presentación debe tener como máximo 30 caracteres");
        }
        String normalizedUnit = contentUnit == null || contentUnit.isBlank() ? null : contentUnit.trim();
        if (normalizedUnit != null && normalizedUnit.length() > 10) {
            throw new BusinessException("INVALID_CONTENT_UNIT", "La unidad de contenido debe tener como máximo 10 caracteres");
        }
        if ((contentQuantity == null) != (normalizedUnit == null)) {
            throw new BusinessException(
                    "INCOMPLETE_CONTENT_DEFINITION",
                    "La cantidad y la unidad de contenido deben informarse juntas");
        }
        if (contentQuantity == null) {
            return;
        }
        if (contentQuantity.signum() <= 0) {
            throw new BusinessException("INVALID_CONTENT_QUANTITY", "La cantidad de contenido debe ser mayor que cero");
        }
        if (Math.max(contentQuantity.stripTrailingZeros().scale(), 0) > 3
                || contentQuantity.precision() - contentQuantity.scale() > 9) {
            throw new BusinessException(
                    "INVALID_CONTENT_QUANTITY_PRECISION",
                    "La cantidad de contenido debe tener como máximo 9 enteros y 3 decimales");
        }
    }

    private static void validateOptionalBarcode(String barcode) {
        if (barcode != null && barcode.isBlank()) {
            throw new BusinessException("INVALID_BARCODE", "El código de barras no puede estar vacío");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new BusinessException("INVALID_PRICE", "El precio debe ser mayor que cero");
        }
        if (Math.max(price.stripTrailingZeros().scale(), 0) > 2) {
            throw new BusinessException("INVALID_PRICE_SCALE", "El precio debe tener como máximo dos decimales");
        }
    }

    private static void validateOptionalPurchasePrice(BigDecimal purchasePrice) {
        if (purchasePrice == null) {
            return;
        }
        if (purchasePrice.signum() <= 0) {
            throw new BusinessException("INVALID_PURCHASE_PRICE", "El precio de compra debe ser mayor que cero");
        }
        if (Math.max(purchasePrice.stripTrailingZeros().scale(), 0) > 2) {
            throw new BusinessException(
                    "INVALID_PURCHASE_PRICE_SCALE",
                    "El precio de compra debe tener como máximo dos decimales");
        }
    }

    private static PageCriteria requirePageCriteria(PageCriteria pageCriteria) {
        if (pageCriteria == null) {
            throw new BusinessException("INVALID_PAGINATION", "La paginación es obligatoria");
        }
        return pageCriteria;
    }
}
