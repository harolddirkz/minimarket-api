package com.minimarket.application.service;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.exception.ForbiddenOperationException;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.SaleIdempotency;
import com.minimarket.domain.model.SaleIdempotencyStatus;
import com.minimarket.domain.model.SaleItem;
import com.minimarket.domain.model.SalePricingPolicy;
import com.minimarket.domain.model.SaleStatus;
import com.minimarket.domain.model.SaleFilter;
import com.minimarket.domain.model.SaleListEntry;
import com.minimarket.domain.model.SaleSummary;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.PreviewSaleCommand;
import com.minimarket.domain.port.in.RegisterSaleCommand;
import com.minimarket.domain.port.in.SaleLineCommand;
import com.minimarket.domain.port.in.SalePreview;
import com.minimarket.domain.port.in.SaleUseCase;
import com.minimarket.domain.port.in.SaleHistoryQuery;
import com.minimarket.domain.port.in.SaleSummaryQuery;
import com.minimarket.domain.port.out.ProductRepositoryPort;
import com.minimarket.domain.port.out.SaleIdempotencyRepositoryPort;
import com.minimarket.domain.port.out.SaleRepositoryPort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SaleService implements SaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final ProductRepositoryPort productRepository;
    private final UserRepositoryPort userRepository;
    private final SaleRepositoryPort saleRepository;
    private final SaleIdempotencyRepositoryPort idempotencyRepository;
    private final Clock clock;

    public SaleService(
            ProductRepositoryPort productRepository,
            UserRepositoryPort userRepository,
            SaleRepositoryPort saleRepository,
            SaleIdempotencyRepositoryPort idempotencyRepository,
            Clock clock) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SalePreview preview(PreviewSaleCommand command) {
        List<SaleLineCommand> lines = requireLines(command == null ? null : command.items());
        return calculate(lines);
    }

    @Override
    @Transactional
    public Sale register(RegisterSaleCommand command) {
        if (command == null || command.userId() == null) {
            throw new BusinessException("INVALID_SALE_USER", "El usuario responsable es obligatorio");
        }
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));
        if (!user.active()) {
            throw new BusinessException("USER_INACTIVE", "El usuario responsable está inactivo");
        }

        String idempotencyKey = requireIdempotencyKey(command.idempotencyKey());
        List<SaleLineCommand> lines = requireLines(command.items());
        String requestHash = requestHash(lines);
        Instant now = clock.instant();

        if (!idempotencyRepository.tryStart(user.id(), idempotencyKey, requestHash, now)) {
            return resolveRepeatedRequest(user.id(), idempotencyKey, requestHash);
        }

        SalePreview calculation = calculate(lines);
        Sale sale = new Sale(
                null,
                saleRepository.nextSaleNumber(),
                user.id(),
                SaleStatus.COMPLETED,
                calculation.total(),
                calculation.items(),
                now,
                now);
        Sale saved = saleRepository.save(sale);
        idempotencyRepository.complete(user.id(), idempotencyKey, saved.id(), clock.instant());
        log.info("Sale registered. saleNumber={} userId={} total={}",
                saved.saleNumber(), saved.userId(), saved.total());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Sale findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("INVALID_SALE_ID", "El identificador de la venta no es válido");
        }
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SALE_NOT_FOUND", "Venta no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public Sale findAccessibleById(Long id, Long requesterId, UserRole requesterRole) {
        if (requesterId == null || requesterRole == null) {
            throw new ForbiddenOperationException("SALE_ACCESS_DENIED", "No tiene acceso a esta venta");
        }
        Sale sale = findById(id);
        if (requesterRole != UserRole.ADMIN && !sale.userId().equals(requesterId)) {
            throw new ForbiddenOperationException("SALE_ACCESS_DENIED", "No tiene acceso a esta venta");
        }
        return sale;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SaleListEntry> findHistory(SaleHistoryQuery query) {
        if (query == null || query.pageCriteria() == null) {
            throw new IllegalArgumentException("La paginación del historial es obligatoria");
        }
        validateDateRange(query.fromInclusive(), query.toExclusive());
        Long effectiveUserId = accessibleHistoryUser(
                query.requestedUserId(), query.requesterId(), query.requesterRole());
        return saleRepository.findAll(
                new SaleFilter(query.fromInclusive(), query.toExclusive(), effectiveUserId, query.status()),
                query.pageCriteria());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleSummary summarize(SaleSummaryQuery query) {
        if (query == null || query.requesterRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("SALE_SUMMARY_ACCESS_DENIED",
                    "No tiene acceso al resumen global de ventas");
        }
        validateDateRange(query.fromInclusive(), query.toExclusive());
        return saleRepository.summarize(new SaleFilter(
                query.fromInclusive(), query.toExclusive(), null, SaleStatus.COMPLETED));
    }

    private SalePreview calculate(List<SaleLineCommand> lines) {
        Set<Long> productIds = lines.stream()
                .map(SaleLineCommand::productId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        List<SaleItem> items = lines.stream()
                .map(line -> priceLine(line, products))
                .toList();
        BigDecimal total = SalePricingPolicy.total(items);
        log.debug("Sale calculated. itemCount={} total={}", items.size(), total);
        return new SalePreview(items, total);
    }

    private SaleItem priceLine(SaleLineCommand line, Map<Long, Product> products) {
        Product product = products.get(line.productId());
        if (product == null) {
            throw new ResourceNotFoundException("PRODUCT_NOT_FOUND",
                    "Producto no encontrado: " + line.productId());
        }
        if (!product.active()) {
            log.warn("Inactive product rejected from sale. productId={}", product.id());
            throw new BusinessException("PRODUCT_INACTIVE", "El producto está inactivo: " + product.id());
        }
        try {
            return SaleItem.priced(product, line.quantity());
        } catch (IllegalArgumentException | ArithmeticException exception) {
            log.warn("Invalid sale quantity. productId={}", product.id());
            throw new BusinessException("INVALID_PRODUCT_QUANTITY",
                    "La cantidad no es válida para el producto: " + product.id());
        }
    }

    private Sale resolveRepeatedRequest(Long userId, String key, String requestHash) {
        SaleIdempotency operation = idempotencyRepository.find(userId, key)
                .orElseThrow(() -> new IllegalStateException("Idempotency operation disappeared"));
        if (!operation.requestHash().equals(requestHash)) {
            log.warn("Idempotency key reused with different request. userId={}", userId);
            throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                    "La clave de idempotencia ya fue utilizada con una solicitud diferente");
        }
        if (operation.status() != SaleIdempotencyStatus.COMPLETED || operation.saleId() == null) {
            throw new ConflictException("SALE_REQUEST_IN_PROGRESS",
                    "La operación de venta todavía está en proceso");
        }
        log.debug("Returning idempotent sale result. saleId={} userId={}", operation.saleId(), userId);
        return saleRepository.findById(operation.saleId())
                .orElseThrow(() -> new IllegalStateException("Completed idempotency operation has no sale"));
    }

    private static List<SaleLineCommand> requireLines(List<SaleLineCommand> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener al menos un producto");
        }
        Set<Long> productIds = new LinkedHashSet<>();
        for (SaleLineCommand line : lines) {
            if (line == null || line.productId() == null || line.productId() <= 0) {
                throw new IllegalArgumentException("El identificador de producto no es válido");
            }
            if (!productIds.add(line.productId())) {
                throw new IllegalArgumentException("La venta no permite productos duplicados");
            }
            try {
                SalePricingPolicy.normalizeQuantity(line.quantity());
            } catch (IllegalArgumentException | ArithmeticException exception) {
                throw new IllegalArgumentException("La cantidad del producto no es válida", exception);
            }
        }
        return List.copyOf(lines);
    }

    private static String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave de idempotencia es obligatoria");
        }
        String normalized = key.trim();
        if (normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException("La clave de idempotencia no puede exceder 128 caracteres");
        }
        return normalized;
    }

    private static String requestHash(List<SaleLineCommand> lines) {
        String canonicalRequest = lines.stream()
                .sorted(Comparator.comparing(SaleLineCommand::productId))
                .map(line -> line.productId() + ":" + SalePricingPolicy.normalizeQuantity(line.quantity()).toPlainString())
                .collect(Collectors.joining("|"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static Long accessibleHistoryUser(Long requestedUserId, Long requesterId, UserRole requesterRole) {
        if (requesterId == null || requesterRole == null) {
            throw new ForbiddenOperationException("SALE_HISTORY_ACCESS_DENIED",
                    "No tiene acceso al historial de ventas");
        }
        if (requestedUserId != null && requestedUserId <= 0) {
            throw new IllegalArgumentException("El filtro de usuario no es válido");
        }
        if (requesterRole == UserRole.ADMIN) {
            return requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.equals(requesterId)) {
            throw new ForbiddenOperationException("SALE_HISTORY_ACCESS_DENIED",
                    "No tiene acceso al historial de otro usuario");
        }
        return requesterId;
    }

    private static void validateDateRange(Instant fromInclusive, Instant toExclusive) {
        if (fromInclusive != null && toExclusive != null && !fromInclusive.isBefore(toExclusive)) {
            throw new IllegalArgumentException("El rango de fechas no es válido");
        }
    }
}
