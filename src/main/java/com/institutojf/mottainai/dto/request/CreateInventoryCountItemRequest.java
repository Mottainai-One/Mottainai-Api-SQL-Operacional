package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateInventoryCountItemRequest(
        @NotNull Integer inventoryId,
        @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal countedQuantity,
        @Size(max = 2000) String observation
) {
}
