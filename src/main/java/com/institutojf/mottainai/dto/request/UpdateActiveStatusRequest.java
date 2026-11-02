package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveStatusRequest(
        @NotNull Boolean active
) {
}
