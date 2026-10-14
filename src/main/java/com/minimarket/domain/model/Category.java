package com.minimarket.domain.model;

public record Category(Long id, String name, String description, boolean active) {

    public Category {
        name = requireText(name, "Category name is required");
        description = normalizeOptional(description);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
