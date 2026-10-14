package com.minimarket.domain.port.in;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.SaleStatus;
import com.minimarket.domain.model.UserRole;

import java.time.Instant;

public record SaleHistoryQuery(
        Instant fromInclusive,
        Instant toExclusive,
        Long requestedUserId,
        SaleStatus status,
        PageCriteria pageCriteria,
        Long requesterId,
        UserRole requesterRole) {
}
