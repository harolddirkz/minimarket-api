package com.minimarket.domain.model;

public record PageCriteria(int page, int size) {

    private static final int MAX_PAGE_SIZE = 100;

    public PageCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}
