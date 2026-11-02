package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.*;

public record CustomerLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
