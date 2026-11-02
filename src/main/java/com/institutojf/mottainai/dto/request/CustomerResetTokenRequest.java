package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerResetTokenRequest(
        @NotBlank String token
) {
}
