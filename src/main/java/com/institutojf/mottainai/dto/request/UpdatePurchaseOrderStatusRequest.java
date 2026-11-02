package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePurchaseOrderStatusRequest(
        @NotNull PurchaseOrderStatus status
) {
}
