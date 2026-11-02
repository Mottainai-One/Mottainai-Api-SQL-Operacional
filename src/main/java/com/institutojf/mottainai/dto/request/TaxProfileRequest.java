package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TaxProfileRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 120) String name,
        String description,
        @Size(max = 4) String cfop,
        @Size(max = 3) String icmsCst,
        @Size(max = 4) String icmsCsosn,
        @NotNull @Digits(integer = 3, fraction = 4) @DecimalMin("0.0000") @DecimalMax("100.0000") BigDecimal icmsRate,
        @Size(max = 2) String ipiCst,
        @NotNull @Digits(integer = 3, fraction = 4) @DecimalMin("0.0000") @DecimalMax("100.0000") BigDecimal ipiRate,
        @Size(max = 2) String pisCst,
        @NotNull @Digits(integer = 3, fraction = 4) @DecimalMin("0.0000") @DecimalMax("100.0000") BigDecimal pisRate,
        @Size(max = 2) String cofinsCst,
        @NotNull @Digits(integer = 3, fraction = 4) @DecimalMin("0.0000") @DecimalMax("100.0000") BigDecimal cofinsRate
) {
}
