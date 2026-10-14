package com.minimarket.domain.port.in;

import java.util.List;

public record PreviewSaleCommand(List<SaleLineCommand> items) {
}
