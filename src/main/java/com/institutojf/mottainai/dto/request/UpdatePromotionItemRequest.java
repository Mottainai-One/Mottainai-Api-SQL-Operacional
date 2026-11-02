package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePromotionItemRequest(
        @NotNull @DecimalMin("0.00") BigDecimal originalPrice,
        @NotNull @DecimalMin("0.00") BigDecimal promotionalPrice,
        @DecimalMin("0.000") BigDecimal quantityAvailable
) {
}
