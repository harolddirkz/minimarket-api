package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.model.UnitOfMeasure;
import com.minimarket.domain.port.in.ProductUseCase;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:30:00Z");

    @Mock
    private ProductUseCase productUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiResponseFactory responseFactory = new ApiResponseFactory();
        ProductController controller = new ProductController(productUseCase, responseFactory);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(
                responseFactory, Clock.fixed(NOW, ZoneOffset.UTC));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void shouldRespectRequestAndResponseContractsWhenCreatingProduct() throws Exception {
        when(productUseCase.create(any())).thenReturn(product());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {
                                    "name": "Leche Gloria",
                                    "barcode": "0012345678905",
                                    "price": 4.50,
                                    "purchasePrice": 3.00,
                                    "presentation": "LATA",
                                    "contentQuantity": 400,
                                    "contentUnit": "GR",
                                    "categoryId": 1,
                                    "unitOfMeasure": "UND"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Producto registrado correctamente"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.barcode").value("0012345678905"))
                .andExpect(jsonPath("$.data.purchasePrice").value(3.00))
                .andExpect(jsonPath("$.data.presentation").value("LATA"))
                .andExpect(jsonPath("$.data.contentQuantity").value(400.000))
                .andExpect(jsonPath("$.data.contentUnit").value("GR"))
                .andExpect(jsonPath("$.data.grossMarginPercentage").value(33.33))
                .andExpect(jsonPath("$.data.markupPercentage").value(50.00));
    }

    @Test
    void shouldRejectRequestWithoutData() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors[0].field").value("data"));
    }

    @Test
    void shouldValidateFieldsInsideData() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {
                                    "name": "Leche",
                                    "price": 0,
                                    "categoryId": 1,
                                    "unitOfMeasure": "UND"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.data.errors[0].field").value("price"));
    }

    @Test
    void shouldRejectIncompleteAndExcessivelyPreciseContent() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":{"name":"Leche","price":4.50,"categoryId":1,
                                "unitOfMeasure":"UND","contentQuantity":400}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":{"name":"Leche","price":4.50,"categoryId":1,
                                "unitOfMeasure":"UND","contentQuantity":1.2345,"contentUnit":"LT"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldTranslateNotFoundExceptionAndMatchHttpStatus() throws Exception {
        when(productUseCase.findActiveById(999L)).thenThrow(
                new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Producto no encontrado"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.data.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.data.timestamp").value("2026-10-08T10:30:00Z"))
                .andExpect(jsonPath("$.data.path").value("/api/v1/products/999"));
    }

    @Test
    void shouldTranslateBusinessException() throws Exception {
        when(productUseCase.findActiveByBarcode("ABC"))
                .thenThrow(new BusinessException("PRODUCT_UNAVAILABLE", "Producto no disponible"));

        mockMvc.perform(get("/api/v1/products/barcode/ABC"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.data.code").value("PRODUCT_UNAVAILABLE"));
    }

    @Test
    void shouldReturnSafeUnexpectedError() throws Exception {
        when(productUseCase.findActiveById(1L)).thenThrow(new RuntimeException("database password leaked"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno inesperado"))
                .andExpect(jsonPath("$.data.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database password leaked"))));
    }

    @Test
    void shouldExposePaginationMetadata() throws Exception {
        when(productUseCase.findAllActive(any()))
                .thenReturn(new PageResult<>(List.of(product()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void shouldReturnNoBodyWhenDeactivatingProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private static Product product() {
        return new Product(
                10L, "Leche Gloria", null, "0012345678905", new BigDecimal("4.50"),
                new BigDecimal("3.00"), "LATA", new BigDecimal("400"), "GR",
                1L, UnitOfMeasure.UND, true, NOW, NOW);
    }
}
