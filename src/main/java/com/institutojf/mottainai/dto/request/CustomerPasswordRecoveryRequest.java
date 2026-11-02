package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.*;

public record CustomerPasswordRecoveryRequest(
        @NotBlank @Email String email
) {
}
