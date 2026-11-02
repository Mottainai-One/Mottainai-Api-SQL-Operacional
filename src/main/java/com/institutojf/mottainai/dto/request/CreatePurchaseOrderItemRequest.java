package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePurchaseOrderItemRequest(
        @NotNull Integer productId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal requestedQuantity,
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitCost
) {
}
