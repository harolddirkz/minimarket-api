package com.minimarket.domain.port.in;

import java.util.List;

public record RegisterSaleCommand(
        Long userId,
        String idempotencyKey,
        List<SaleLineCommand> items) {
}
