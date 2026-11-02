package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record InventoryCountItemResponse(
        Long id,
        Integer inventoryId,
        BigDecimal systemQuantity,
        BigDecimal countedQuantity,
        BigDecimal difference,
        String observation
) {
}
