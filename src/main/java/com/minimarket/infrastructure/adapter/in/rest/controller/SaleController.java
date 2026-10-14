package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.SaleUseCase;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.SaleStatus;
import com.minimarket.domain.port.in.SaleHistoryQuery;
import com.minimarket.domain.port.in.SaleSummaryQuery;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SalePreviewResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.PageResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleListResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.SaleSummaryResponse;
import com.minimarket.infrastructure.adapter.in.rest.mapper.SaleRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Validated
@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private final SaleUseCase useCase;
    private final ApiResponseFactory responseFactory;

    public SaleController(SaleUseCase useCase, ApiResponseFactory responseFactory) {
        this.useCase = useCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<SalePreviewResponse>> preview(
            @Valid @RequestBody ApiRequest<SaleRequest> request) {
        return responseFactory.ok("Venta calculada correctamente",
                SaleRestMapper.toResponse(useCase.preview(SaleRestMapper.toPreviewCommand(request.data()))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponse>> register(
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "La clave de idempotencia es obligatoria")
            @Size(max = 128, message = "La clave de idempotencia no puede exceder 128 caracteres")
            String idempotencyKey,
            @Valid @RequestBody ApiRequest<SaleRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        return responseFactory.created("Venta registrada correctamente", SaleRestMapper.toResponse(
                useCase.register(SaleRestMapper.toRegisterCommand(
                        request.data(), userId(jwt), idempotencyKey))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponse>> findById(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return responseFactory.ok("Venta encontrada correctamente", SaleRestMapper.toResponse(
                useCase.findAccessibleById(id, userId(jwt), role(jwt))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SaleListResponse>>> findHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal Jwt jwt) {
        var query = new SaleHistoryQuery(
                startOfDay(from), endOfDayExclusive(to), userId, status,
                new PageCriteria(page, size), userId(jwt), role(jwt));
        return responseFactory.ok("Historial de ventas obtenido correctamente",
                SaleRestMapper.toResponse(useCase.findHistory(query)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SaleSummaryResponse>> summarize(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {
        var query = new SaleSummaryQuery(
                startOfDay(from), endOfDayExclusive(to), userId(jwt), role(jwt));
        return responseFactory.ok("Resumen de ventas obtenido correctamente",
                SaleRestMapper.toResponse(useCase.summarize(query)));
    }

    private static long userId(Jwt jwt) {
        Number value = jwt.getClaim("user_id");
        return value.longValue();
    }

    private static UserRole role(Jwt jwt) {
        return UserRole.valueOf(jwt.getClaimAsString("role"));
    }

    private static Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private static Instant endOfDayExclusive(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    }
}
