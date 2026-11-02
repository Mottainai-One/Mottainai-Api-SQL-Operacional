package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchResponse(
        Integer id,
        Integer productId,
        String batchCode,
        LocalDate manufactureDate,
        LocalDate expirationDate,
        BigDecimal initialQuantity,
        BigDecimal unitCost,
        Boolean active,
        Integer version
) {
}
