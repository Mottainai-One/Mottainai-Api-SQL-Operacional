package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.*;

public record CreateCustomerGeofenceRequest(
        @NotNull Integer storeId,
        @NotNull @Min(50) @Max(10000) Integer radiusMeters
) {
}
