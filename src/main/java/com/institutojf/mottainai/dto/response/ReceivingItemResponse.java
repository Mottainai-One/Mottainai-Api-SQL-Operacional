package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivingItemResponse(
        Integer id,
        Integer purchaseOrderItemId,
        Integer productId,
        BigDecimal requestedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal unitCost,
        LocalDate manufactureDate,
        LocalDate expirationDate,
        String observation
) {
}
