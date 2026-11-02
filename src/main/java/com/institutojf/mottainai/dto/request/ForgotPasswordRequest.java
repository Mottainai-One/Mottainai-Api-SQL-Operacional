package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequest(
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @NotBlank @Email String email
) {
}
