package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        Integer storeId,
        String month,
        long transactionCount,
        BigDecimal totalAmount
) {
}
