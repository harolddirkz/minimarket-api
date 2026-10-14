package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.port.in.ProductUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.PageResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductCreateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductPriceUpdateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.ProductUpdateRequest;
import com.minimarket.infrastructure.adapter.in.rest.mapper.ProductRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductUseCase productUseCase;
    private final ApiResponseFactory responseFactory;

    public ProductController(ProductUseCase productUseCase, ApiResponseFactory responseFactory) {
        this.productUseCase = productUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ApiRequest<ProductCreateRequest> request) {
        ProductResponse response = ProductRestMapper.toResponse(
                productUseCase.create(ProductRestMapper.toCommand(request.data())));
        return responseFactory.created("Producto registrado correctamente", response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> findAllActive(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResponse<ProductResponse> response = ProductRestMapper.toResponse(
                productUseCase.findAllActive(new PageCriteria(page, size)));
        return responseFactory.ok("Productos encontrados correctamente", response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable @Positive Long id) {
        return responseFactory.ok(
                "Producto encontrado correctamente",
                ProductRestMapper.toResponse(productUseCase.findActiveById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchByName(
            @RequestParam @Size(min = 1, max = 160) @Pattern(regexp = ".*\\S.*") String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResponse<ProductResponse> response = ProductRestMapper.toResponse(
                productUseCase.searchActiveByName(name, new PageCriteria(page, size)));
        return responseFactory.ok("Búsqueda de productos completada correctamente", response);
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<ProductResponse>> findByBarcode(
            @PathVariable @Size(min = 1, max = 64) @Pattern(regexp = "\\S+") String barcode) {
        return responseFactory.ok(
                "Producto encontrado correctamente",
                ProductRestMapper.toResponse(productUseCase.findActiveByBarcode(barcode)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApiRequest<ProductUpdateRequest> request) {
        ProductResponse response = ProductRestMapper.toResponse(
                productUseCase.update(id, ProductRestMapper.toCommand(request.data())));
        return responseFactory.ok("Producto actualizado correctamente", response);
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ApiResponse<ProductResponse>> updatePrice(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApiRequest<ProductPriceUpdateRequest> request) {
        ProductResponse response = ProductRestMapper.toResponse(
                productUseCase.updatePrice(id, request.data().price()));
        return responseFactory.ok("Precio actualizado correctamente", response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable @Positive Long id) {
        productUseCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
