package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateStoreUserRequest(
        @Pattern(regexp = "ADMINISTRATOR|MANAGER|SUPERVISOR|OPERATOR|INTERN") String role,
        Boolean active
) {
}
