package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.enums.InventoryType;

import java.math.BigDecimal;

public record InventoryResponse(
        Integer id,
        Integer storeId,
        Integer batchId,
        InventoryType inventoryType,
        BigDecimal currentQuantity,
        BigDecimal minimumQuantity,
        BigDecimal maximumQuantity,
        String location,
        Integer version,
        boolean reactivated
) {
}
