package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.*;

public record CustomerPasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
