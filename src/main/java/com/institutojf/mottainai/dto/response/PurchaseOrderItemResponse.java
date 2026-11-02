package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Integer id,
        Integer productId,
        BigDecimal requestedQuantity,
        BigDecimal unitCost,
        BigDecimal subtotal
) {
}
