package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReceivingItemRequest(
        @NotNull Integer purchaseOrderItemId,
        @NotNull @DecimalMin("0.001") BigDecimal receivedQuantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitCost,
        LocalDate manufactureDate,
        @NotNull LocalDate expirationDate,
        String observation
) {
}
