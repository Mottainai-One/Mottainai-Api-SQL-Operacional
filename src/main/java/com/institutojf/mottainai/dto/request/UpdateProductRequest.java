package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotNull Integer categoryId,
        @NotNull Integer taxProfileId,
        @NotBlank @Pattern(regexp = "\\d{8}") String ncm,
        @Pattern(regexp = "\\d{7}") String cest,
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 100) String brand,
        @NotBlank @Size(max = 20) String unitMeasure,
        @DecimalMin(value = "0.000") @Digits(integer = 7, fraction = 3) BigDecimal weight,
        @NotNull Boolean active,
        @NotNull Integer version,
        @jakarta.validation.constraints.Pattern(regexp = "^https://res\\.cloudinary\\.com/[^/]+/image/upload/.{1,1900}$") String imageUrl
) {
}
