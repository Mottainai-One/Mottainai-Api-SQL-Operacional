package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBatchRequest(
        @NotNull Integer productId,
        Integer receivingItemId,
        @NotBlank @Size(max = 60) String batchCode,
        @PastOrPresent LocalDate manufactureDate,
        @NotNull LocalDate expirationDate,
        @NotNull @DecimalMin(value = "0.001") @Digits(integer = 7, fraction = 3) BigDecimal initialQuantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal unitCost
) {
}
