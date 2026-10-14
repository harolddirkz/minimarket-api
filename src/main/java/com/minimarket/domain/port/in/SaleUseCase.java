package com.minimarket.domain.port.in;

import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.SaleListEntry;
import com.minimarket.domain.model.SaleSummary;

public interface SaleUseCase {

    SalePreview preview(PreviewSaleCommand command);

    Sale register(RegisterSaleCommand command);

    Sale findById(Long id);

    Sale findAccessibleById(Long id, Long requesterId, UserRole requesterRole);

    PageResult<SaleListEntry> findHistory(SaleHistoryQuery query);

    SaleSummary summarize(SaleSummaryQuery query);
}
