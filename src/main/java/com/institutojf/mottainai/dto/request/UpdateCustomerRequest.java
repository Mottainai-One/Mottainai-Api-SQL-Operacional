package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        LocalDate birthDate,
        @NotNull Boolean marketingConsent,
        @Valid CreateAddressRequest address
) {
}
