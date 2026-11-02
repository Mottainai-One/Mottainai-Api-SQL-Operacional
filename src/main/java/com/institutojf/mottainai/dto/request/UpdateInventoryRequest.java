package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateInventoryRequest(
        @NotNull @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal minimumQuantity,
        @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal maximumQuantity,
        @Size(max = 80) String location,
        @NotNull Integer version
) {
}
