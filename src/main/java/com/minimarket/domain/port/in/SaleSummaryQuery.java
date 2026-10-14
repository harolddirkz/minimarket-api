package com.minimarket.domain.port.in;

import com.minimarket.domain.model.UserRole;

import java.time.Instant;

public record SaleSummaryQuery(
        Instant fromInclusive,
        Instant toExclusive,
        Long requesterId,
        UserRole requesterRole) {
}
