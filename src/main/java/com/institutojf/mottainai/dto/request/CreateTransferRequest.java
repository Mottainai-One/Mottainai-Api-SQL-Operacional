package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateTransferRequest(
        @NotNull Integer destinationStoreId,
        Integer suggestedActionId,
        String observation,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull Integer batchId,
            @NotNull @Positive BigDecimal transferredQuantity
    ) {
    }
}
