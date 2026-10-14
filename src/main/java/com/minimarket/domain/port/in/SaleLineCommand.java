package com.minimarket.domain.port.in;

import java.math.BigDecimal;

public record SaleLineCommand(Long productId, BigDecimal quantity) {
}
