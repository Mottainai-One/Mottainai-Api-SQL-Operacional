package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.MovementType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateInventoryMovementRequest(
        @NotNull MovementType movementType,
        @NotNull @Digits(integer = 7, fraction = 3) BigDecimal movedQuantity,
        @Size(max = 2000) String observation
) {
}
