package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateReplenishmentExecutionRequest(
        @NotNull Integer preListId,
        @Min(1) @Max(5) Integer rating,
        String comment,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull Integer batchId,
            @NotNull @Positive BigDecimal replenishedQuantity
    ) {
    }
}
