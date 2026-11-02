package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductPriceHistoryResponse(
        Long id,
        Integer productId,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Integer changedBy,
        LocalDateTime changedAt
) {
}
