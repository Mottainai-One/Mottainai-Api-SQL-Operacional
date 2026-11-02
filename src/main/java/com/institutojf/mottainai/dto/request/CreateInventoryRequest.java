package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.InventoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateInventoryRequest(
        Integer storeId,
        @NotNull Integer batchId,
        InventoryType inventoryType,
        @NotNull @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal minimumQuantity,
        @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal maximumQuantity,
        @Size(max = 80) String location
) {
}
