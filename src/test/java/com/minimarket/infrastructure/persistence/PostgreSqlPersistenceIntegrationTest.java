package com.minimarket.infrastructure.persistence;

import com.minimarket.domain.model.Category;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.Product;
import com.minimarket.domain.model.RefreshToken;
import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.SaleIdempotencyStatus;
import com.minimarket.domain.model.SaleFilter;
import com.minimarket.domain.model.SaleSummary;
import com.minimarket.domain.model.SaleItem;
import com.minimarket.domain.model.SalePricingPolicy;
import com.minimarket.domain.model.SaleStatus;
import com.minimarket.domain.model.UnitOfMeasure;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.in.CategoryUseCase;
import com.minimarket.domain.port.in.CreateCategoryCommand;
import com.minimarket.domain.port.in.CreateProductCommand;
import com.minimarket.domain.port.in.ProductUseCase;
import com.minimarket.domain.port.in.UpdateProductCommand;
import com.minimarket.domain.port.in.RefreshSessionCommand;
import com.minimarket.domain.port.in.RefreshSessionResult;
import com.minimarket.domain.port.in.SessionUseCase;
import com.minimarket.domain.port.in.RegisterSaleCommand;
import com.minimarket.domain.port.in.SaleLineCommand;
import com.minimarket.domain.port.in.SaleUseCase;
import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.port.out.RefreshTokenRepositoryPort;
import com.minimarket.domain.port.out.SaleIdempotencyRepositoryPort;
import com.minimarket.domain.port.out.SaleRepositoryPort;
import com.minimarket.domain.port.out.AccessTokenPort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import com.minimarket.domain.port.out.RefreshTokenValuePort;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostgreSqlPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("minimarket_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private CategoryUseCase categoryUseCase;

    @Autowired
    private ProductUseCase productUseCase;

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @Autowired
    private AccessTokenPort accessTokenPort;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private SessionUseCase sessionUseCase;

    @Autowired
    private RefreshTokenValuePort refreshTokenValuePort;

    @Autowired
    private SaleRepositoryPort saleRepository;

    @Autowired
    private SaleIdempotencyRepositoryPort saleIdempotencyRepository;

    @Autowired
    private SaleUseCase saleUseCase;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void shouldApplyMigrationsAndPersistSearchableCatalogData() {
        assertThat(flyway.info().applied()).hasSize(8);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");

        Category category = categoryUseCase.create(new CreateCategoryCommand("Abarrotes", null));
        Product product = productUseCase.create(new CreateProductCommand(
                "Arroz Costeño", null, "7750000000001", new BigDecimal("4.80"),
                new BigDecimal("3.20"), "BOLSA", new BigDecimal("1"), "KG",
                "Extra", 1, null, null,
                category.id(), UnitOfMeasure.KG));

        PageResult<Product> search = productUseCase.searchActiveByName(
                "ARROZ", new PageCriteria(0, 20));

        assertThat(product.id()).isNotNull();
        assertThat(product.purchasePrice()).isEqualByComparingTo("3.20");
        assertThat(product.presentation()).isEqualTo("BOLSA");
        assertThat(product.contentQuantity()).isEqualByComparingTo("1.000");
        assertThat(product.contentUnit()).isEqualTo("KG");
        assertThat(product.variant()).isEqualTo("Extra");
        assertThat(product.unitsPerPackage()).isEqualTo(1);
        assertThat(search.content()).extracting(Product::id).containsExactly(product.id());
        assertThat(productUseCase.findActiveByBarcode("7750000000001").id()).isEqualTo(product.id());
    }

    @Test
    @Transactional
    void shouldPersistSaleSnapshotsAndIdempotencyState() {
        Instant now = Instant.parse("2026-10-08T15:00:00Z");
        User user = userRepository.save(new User(null, "sales-user", "$2a$12$persistenceHash",
                "Sales User", UserRole.VENDEDOR, true, now, now));
        Category category = categoryUseCase.create(new CreateCategoryCommand("Ventas", null));
        Product product = productUseCase.create(new CreateProductCommand(
                "Tomate", null, "7750000000099", new BigDecimal("4.55"),
                new BigDecimal("3.00"), "BOLSA", new BigDecimal("1.250"), "KG",
                category.id(), UnitOfMeasure.KG));
        SaleItem item = SaleItem.priced(product, new BigDecimal("0.555"));
        String saleNumber = saleRepository.nextSaleNumber();
        Sale sale = new Sale(null, saleNumber, user.id(), SaleStatus.COMPLETED,
                SalePricingPolicy.total(java.util.List.of(item)), java.util.List.of(item), now, now);

        assertThat(saleIdempotencyRepository.tryStart(
                user.id(), "device-operation-1", "a".repeat(64), now)).isTrue();
        assertThat(saleIdempotencyRepository.tryStart(
                user.id(), "device-operation-1", "a".repeat(64), now)).isFalse();

        Sale persisted = saleRepository.save(sale);
        saleIdempotencyRepository.complete(user.id(), "device-operation-1", persisted.id(), now);

        assertThat(persisted.id()).isNotNull();
        assertThat(persisted.saleNumber()).matches("V-\\d{8}");
        assertThat(persisted.total()).isEqualByComparingTo("2.53");
        assertThat(persisted.items()).singleElement().satisfies(savedItem -> {
            assertThat(savedItem.productName()).isEqualTo("Tomate");
            assertThat(savedItem.productPresentation()).isEqualTo("BOLSA");
            assertThat(savedItem.productContentQuantity()).isEqualByComparingTo("1.250");
            assertThat(savedItem.productContentUnit()).isEqualTo("KG");
            assertThat(savedItem.unitPrice()).isEqualByComparingTo("4.55");
            assertThat(savedItem.quantity()).isEqualByComparingTo("0.555");
        });
        assertThat(saleRepository.findById(persisted.id())).contains(persisted);
        assertThat(saleIdempotencyRepository.find(user.id(), "device-operation-1"))
                .get()
                .satisfies(operation -> {
                    assertThat(operation.status()).isEqualTo(SaleIdempotencyStatus.COMPLETED);
                    assertThat(operation.saleId()).isEqualTo(persisted.id());
                });

        SaleSummary completedBeforeCancellation = saleRepository.summarize(
                new SaleFilter(null, null, null, SaleStatus.COMPLETED));
        saleRepository.save(new Sale(null, saleRepository.nextSaleNumber(), user.id(), SaleStatus.CANCELLED,
                SalePricingPolicy.total(java.util.List.of(item)), java.util.List.of(item), now, now));
        SaleSummary completedAfterCancellation = saleRepository.summarize(
                new SaleFilter(null, null, null, SaleStatus.COMPLETED));
        assertThat(completedAfterCancellation).isEqualTo(completedBeforeCancellation);

        SaleSummary emptyRange = saleRepository.summarize(new SaleFilter(
                Instant.parse("2100-01-01T00:00:00Z"), null, null, SaleStatus.COMPLETED));
        assertThat(emptyRange.totalSales()).isZero();
        assertThat(emptyRange.totalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnOneSaleForConcurrentIdempotentRegistrationsAndKeepHistoricalPrice() throws Exception {
        Instant now = Instant.now();
        User user = userRepository.save(new User(null, "concurrent-sales-user", "$2a$12$persistenceHash",
                "Concurrent Sales User", UserRole.VENDEDOR, true, now, now));
        Category category = categoryUseCase.create(new CreateCategoryCommand("Concurrent Sales", null));
        Product product = productUseCase.create(new CreateProductCommand(
                "Arroz a granel", null, "7750000000198", new BigDecimal("5.20"),
                new BigDecimal("4.00"), "BOLSA", new BigDecimal("1"), "KG",
                category.id(), UnitOfMeasure.KG));
        RegisterSaleCommand command = new RegisterSaleCommand(
                user.id(), "concurrent-sale-operation",
                java.util.List.of(new SaleLineCommand(product.id(), new BigDecimal("1.250"))));

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Sale> first = executor.submit(() -> registerConcurrently(start, command));
            Future<Sale> second = executor.submit(() -> registerConcurrently(start, command));
            start.countDown();

            Sale firstResult = first.get();
            Sale secondResult = second.get();
            assertThat(firstResult.id()).isEqualTo(secondResult.id());
            assertThat(firstResult.saleNumber()).isEqualTo(secondResult.saleNumber());
            assertThat(firstResult.total()).isEqualByComparingTo("6.50");

            productUseCase.update(product.id(), new UpdateProductCommand(
                    product.name(), product.description(), product.barcode(), new BigDecimal("6.00"),
                    product.purchasePrice(), "CAJA", new BigDecimal("2"), "KG",
                    product.categoryId(), product.unitOfMeasure()));
            Sale historical = saleUseCase.findById(firstResult.id());
            assertThat(historical.items()).singleElement().satisfies(item -> {
                assertThat(item.unitPrice()).isEqualByComparingTo("5.20");
                assertThat(item.subtotal()).isEqualByComparingTo("6.50");
                assertThat(item.productPresentation()).isEqualTo("BOLSA");
                assertThat(item.productContentQuantity()).isEqualByComparingTo("1.000");
                assertThat(item.productContentUnit()).isEqualTo("KG");
            });

            RegisterSaleCommand invalid = new RegisterSaleCommand(
                    user.id(), "rollback-sale-operation",
                    java.util.List.of(new SaleLineCommand(999999L, BigDecimal.ONE)));
            assertThatThrownBy(() -> saleUseCase.register(invalid))
                    .isInstanceOf(ResourceNotFoundException.class);
            Sale afterRollback = saleUseCase.register(new RegisterSaleCommand(
                    user.id(), "rollback-sale-operation",
                    java.util.List.of(new SaleLineCommand(product.id(), BigDecimal.ONE))));
            assertThat(afterRollback.id()).isNotNull();
        }
    }

    private Sale registerConcurrently(CountDownLatch start, RegisterSaleCommand command) throws InterruptedException {
        start.await();
        return saleUseCase.register(command);
    }

    @Test
    @Transactional
    void shouldPersistUsersAndRevocableRefreshTokenFamilies() {
        Instant now = Instant.parse("2026-10-08T15:00:00Z");
        User user = userRepository.save(new User(
                null, " Admin01 ", "$2a$12$opaqueHashForPersistenceOnly", "Administrador Principal",
                UserRole.ADMIN, true, now, now));

        UUID familyId = UUID.randomUUID();
        String tokenHash = "a".repeat(64);
        RefreshToken token = refreshTokenRepository.save(new RefreshToken(
                null, user.id(), tokenHash, familyId,
                now.plus(30, ChronoUnit.DAYS), null, now));

        assertThat(user.id()).isNotNull();
        assertThat(user.username()).isEqualTo("Admin01");
        assertThat(userRepository.findByUsername("  ADMIN01 ")).contains(user);
        assertThat(userRepository.existsByUsername("admin01")).isTrue();
        assertThat(userRepository.countActiveAdministrators()).isEqualTo(1);
        assertThat(userRepository.countAdministrators()).isEqualTo(1);
        assertThat(token.id()).isNotNull();
        assertThat(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).contains(token);

        Instant revokedAt = now.plus(1, ChronoUnit.HOURS);
        assertThat(refreshTokenRepository.revokeActiveByFamilyId(familyId, revokedAt)).isEqualTo(1);
        assertThat(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .get()
                .extracting(RefreshToken::revokedAt)
                .isEqualTo(revokedAt);

        String accessToken = accessTokenPort.issue(user).value();
        assertThat(jwtDecoder.decode(accessToken).getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwtDecoder.decode(accessToken).getClaimAsString("token_type")).isEqualTo("access");
    }

    @Test
    void shouldAllowOnlyOneConcurrentRotationAndRevokeFamilyOnReuse() throws Exception {
        Instant now = Instant.now();
        User user = userRepository.save(new User(
                null, "concurrent-user", "$2a$12$opaqueHashForPersistenceOnly", "Concurrent User",
                UserRole.VENDEDOR, true, now, now));
        String rawToken = refreshTokenValuePort.generate();
        refreshTokenRepository.save(new RefreshToken(
                null, user.id(), refreshTokenValuePort.hash(rawToken), UUID.randomUUID(),
                now.plus(30, ChronoUnit.DAYS), null, now));

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Object> first = executor.submit(() -> refreshConcurrently(start, rawToken));
            Future<Object> second = executor.submit(() -> refreshConcurrently(start, rawToken));
            start.countDown();

            Object firstResult = resultOrCause(first);
            Object secondResult = resultOrCause(second);
            assertThat(java.util.List.of(firstResult, secondResult))
                    .filteredOn(RefreshSessionResult.class::isInstance)
                    .hasSize(1);
            assertThat(java.util.List.of(firstResult, secondResult))
                    .filteredOn(AuthenticationFailedException.class::isInstance)
                    .hasSize(1);

            RefreshSessionResult successful = (RefreshSessionResult) java.util.List.of(firstResult, secondResult)
                    .stream().filter(RefreshSessionResult.class::isInstance).findFirst().orElseThrow();
            assertThatThrownBy(() -> sessionUseCase.refresh(
                    new RefreshSessionCommand(successful.refreshToken())))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
    }

    private Object refreshConcurrently(CountDownLatch start, String rawToken) throws InterruptedException {
        start.await();
        try {
            return sessionUseCase.refresh(new RefreshSessionCommand(rawToken));
        } catch (AuthenticationFailedException exception) {
            return exception;
        }
    }

    private static Object resultOrCause(Future<Object> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            throw (Exception) exception.getCause();
        }
    }

    @Test
    void shouldEnforceAuthenticationAndRoleMatrixWithStandardErrors() throws Exception {
        mockMvc.perform(get("/swagger.yml"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/v1/products").header("X-Correlation-ID", "security-test-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "security-test-401"))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data.traceId").value("security-test-401"));

        mockMvc.perform(get("/api/v1/products")
                        .with(jwt().jwt(token -> token.claim("user_id", 20L))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().jwt(token -> token.claim("user_id", 20L))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.data.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().jwt(token -> token.claim("user_id", 20L))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden());

        String createUserBody = """
                {"data":{"username":"security-created-user","password":"StrongPassword1!",
                "fullName":"Security Created User","role":"VENDEDOR"}}
                """;
        mockMvc.perform(post("/api/v1/users")
                        .with(jwt().jwt(token -> token.claim("user_id", 20L))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .contentType("application/json")
                        .content(createUserBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().jwt(token -> token.claim("user_id", 1L))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users")
                        .with(jwt().jwt(token -> token.claim("user_id", 1L))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content(createUserBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("security-created-user"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        String validLogin = """
                {"data":{"username":"security-created-user","password":"StrongPassword1!"}}
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json").content(validLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());

        String invalidLogin = """
                {"data":{"username":"security-created-user","password":"IncorrectPassword1!"}}
                """;
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json").content(invalidLogin))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.data.code").value("INVALID_CREDENTIALS"));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json").content(validLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldExposeSaleEndpointsAndEnforceSaleOwnership() throws Exception {
        Instant now = Instant.now();
        User seller = userRepository.save(new User(null, "sale-api-seller", "$2a$12$persistenceHash",
                "Sale API Seller", UserRole.VENDEDOR, true, now, now));
        User anotherSeller = userRepository.save(new User(null, "sale-api-other", "$2a$12$persistenceHash",
                "Sale API Other", UserRole.VENDEDOR, true, now, now));
        Category category = categoryUseCase.create(new CreateCategoryCommand("Sale API", null));
        Product product = productUseCase.create(new CreateProductCommand(
                "Leche API", null, "7750000000297", new BigDecimal("4.50"),
                new BigDecimal("3.00"), category.id(), UnitOfMeasure.UND));
        String body = """
                {"data":{"items":[{"productId":%d,"quantity":2}]}}
                """.formatted(product.id());

        mockMvc.perform(post("/api/v1/sales/preview")
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.total").value(9.00));

        String response = mockMvc.perform(post("/api/v1/sales")
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .header("Idempotency-Key", "sale-api-operation")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.userId").value(seller.id()))
                .andReturn().getResponse().getContentAsString();
        Number saleId = JsonPath.read(response, "$.data.id");
        String saleDate = LocalDate.now(ZoneId.of("America/Lima")).toString();

        mockMvc.perform(get("/api/v1/sales")
                        .queryParam("from", saleDate).queryParam("to", saleDate)
                        .queryParam("page", "0").queryParam("size", "20")
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(saleId.longValue()))
                .andExpect(jsonPath("$.data.content[0].items").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/sales")
                        .queryParam("userId", anotherSeller.id().toString())
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("SALE_HISTORY_ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/sales/summary")
                        .queryParam("from", saleDate).queryParam("to", saleDate)
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "ADMIN"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalAmount").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(9.0)))
                .andExpect(jsonPath("$.data.currency").value("PEN"));

        mockMvc.perform(get("/api/v1/sales/summary")
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/sales/{id}", saleId.longValue())
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sales/{id}", saleId.longValue())
                        .with(jwt().jwt(token -> token.claim("user_id", anotherSeller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("SALE_ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/sales/{id}", saleId.longValue())
                        .with(jwt().jwt(token -> token.claim("user_id", anotherSeller.id()).claim("role", "ADMIN"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sales")
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("INVALID_PARAMETER"));

        mockMvc.perform(post("/api/v1/sales/preview")
                        .contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/sales/preview")
                        .with(jwt().jwt(token -> token.claim("user_id", seller.id()).claim("role", "VENDEDOR"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                        .contentType("application/json")
                        .content("{\"data\":{\"items\":[]}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }
}
