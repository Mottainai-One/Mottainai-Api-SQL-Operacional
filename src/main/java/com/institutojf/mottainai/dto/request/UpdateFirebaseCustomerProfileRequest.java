package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateFirebaseCustomerProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Schema(description = "Omit to preserve the stored phone token; an empty string explicitly removes it")
        @Size(max = 20) String phone,
        @NotNull Boolean marketingConsent
) {
}
