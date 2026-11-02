package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull Integer supplierId,
        LocalDate expectedDeliveryDate,
        @Size(max = 2000) String observation,
        @NotEmpty List<@Valid CreatePurchaseOrderItemRequest> items
) {
}
