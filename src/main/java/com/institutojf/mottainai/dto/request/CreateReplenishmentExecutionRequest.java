package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

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
            @Schema(name = "replenishedQuantity", description = "Quantity replenished for this batch", example = "3")
            @NotNull @Positive BigDecimal replenishedQuantity
    ) {
    }
}
