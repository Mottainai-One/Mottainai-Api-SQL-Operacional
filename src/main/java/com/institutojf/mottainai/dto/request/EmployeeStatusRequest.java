package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(
        @NotNull Boolean active
) {
}
