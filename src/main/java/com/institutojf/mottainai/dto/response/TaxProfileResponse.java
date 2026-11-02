package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxProfileResponse(
        Integer id,
        String code,
        String name,
        String description,
        String cfop,
        String icmsCst,
        String icmsCsosn,
        BigDecimal icmsRate,
        String ipiCst,
        BigDecimal ipiRate,
        String pisCst,
        BigDecimal pisRate,
        String cofinsCst,
        BigDecimal cofinsRate,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
