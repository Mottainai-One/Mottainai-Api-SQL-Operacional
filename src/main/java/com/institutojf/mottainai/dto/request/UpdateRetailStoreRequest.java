package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.math.BigDecimal;

public record UpdateRetailStoreRequest(
        @NotNull @Valid UpdateAddressRequest address,
        @NotBlank @Size(max = 120) String name,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull Boolean active
) {
}
