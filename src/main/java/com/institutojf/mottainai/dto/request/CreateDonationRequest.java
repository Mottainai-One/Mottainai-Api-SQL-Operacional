package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateDonationRequest(
        @NotBlank @Size(max = 150) String institution,
        Integer suggestedActionId,
        String observation,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull Integer batchId,
            @NotNull @Positive BigDecimal donatedQuantity
    ) {
    }
}
