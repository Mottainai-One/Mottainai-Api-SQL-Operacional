package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryMovementResponse(
        Integer id,
        Integer inventoryId,
        Integer employeeId,
        LocalDateTime movementDate,
        MovementType movementType,
        BigDecimal movedQuantity,
        BigDecimal previousBalance,
        BigDecimal currentBalance,
        String observation,
        Integer storeId
) {
}
