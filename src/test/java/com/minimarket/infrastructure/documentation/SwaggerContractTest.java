package com.minimarket.infrastructure.documentation;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerContractTest {

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    @Test
    void shouldContainAValidAndSelfContainedApiContract() {
        Map<String, Object> document = loadContract();

        assertThat(document.get("openapi")).isEqualTo("3.1.0");
        assertThat(paths(document).keySet()).containsExactlyInAnyOrder(
                "/api/v1/system/info",
                "/api/v1/categories",
                "/api/v1/products",
                "/api/v1/products/{id}",
                "/api/v1/products/search",
                "/api/v1/products/barcode/{barcode}",
                "/api/v1/products/{id}/price",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout",
                "/api/v1/auth/me",
                "/api/v1/users",
                "/api/v1/users/{id}",
                "/api/v1/users/{id}/status",
                "/api/v1/users/{id}/role",
                "/api/v1/users/{id}/reset-password",
                "/api/v1/sales/preview",
                "/api/v1/sales",
                "/api/v1/sales/summary",
                "/api/v1/sales/{id}");
        assertThat(countOperations(paths(document))).isEqualTo(26);

        assertThat(document).containsKey("security");
        assertThat(components(document, "securitySchemes")).containsKey("bearerAuth");
        assertThat(operation(document, "/api/v1/auth/login", "post").get("security"))
                .isEqualTo(List.of());
        assertThat(operation(document, "/api/v1/auth/refresh", "post").get("security"))
                .isEqualTo(List.of());
        assertThat(operation(document, "/api/v1/sales", "post").get("parameters").toString())
                .contains("IdempotencyKey");
        assertThat(operation(document, "/api/v1/sales/summary", "get").get("description").toString())
                .contains("ADMIN", "PostgreSQL");

        assertLocalReferencesResolve(document, document);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> components(Map<String, Object> document, String section) {
        return (Map<String, Object>) ((Map<String, Object>) document.get("components")).get(section);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> operation(
            Map<String, Object> document, String path, String method) {
        return (Map<String, Object>) ((Map<String, Object>) paths(document).get(path)).get(method);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadContract() {
        try (InputStream input = SwaggerContractTest.class.getResourceAsStream("/swagger.yml")) {
            assertThat(input).as("swagger.yml must be available on the classpath").isNotNull();
            return new Yaml().load(input);
        } catch (Exception exception) {
            throw new AssertionError("Unable to load swagger.yml", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> paths(Map<String, Object> document) {
        return (Map<String, Object>) document.get("paths");
    }

    @SuppressWarnings("unchecked")
    private static long countOperations(Map<String, Object> paths) {
        return paths.values().stream()
                .map(value -> (Map<String, Object>) value)
                .flatMap(path -> path.keySet().stream())
                .filter(HTTP_METHODS::contains)
                .count();
    }

    @SuppressWarnings("unchecked")
    private static void assertLocalReferencesResolve(Object value, Map<String, Object> document) {
        if (value instanceof Map<?, ?> map) {
            Object reference = map.get("$ref");
            if (reference instanceof String ref && ref.startsWith("#/")) {
                assertThat(resolveReference(document, ref)).as("reference %s", ref).isNotNull();
            }
            map.values().forEach(item -> assertLocalReferencesResolve(item, document));
        } else if (value instanceof List<?> list) {
            list.forEach(item -> assertLocalReferencesResolve(item, document));
        }
    }

    @SuppressWarnings("unchecked")
    private static Object resolveReference(Map<String, Object> document, String reference) {
        Object current = document;
        for (String segment : reference.substring(2).split("/")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(segment.replace("~1", "/").replace("~0", "~"));
        }
        return current;
    }
}
