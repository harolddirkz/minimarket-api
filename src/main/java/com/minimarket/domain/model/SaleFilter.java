package com.minimarket.domain.model;

import java.time.Instant;

public record SaleFilter(
        Instant fromInclusive,
        Instant toExclusive,
        Long userId,
        SaleStatus status) {
}
