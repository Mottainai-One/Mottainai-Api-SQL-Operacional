package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.enums.ReceivingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReceivingResponse(
        Integer id,
        Integer purchaseOrderId,
        Integer storeId,
        Integer employeeId,
        LocalDateTime receivingDate,
        ReceivingStatus status,
        String observation,
        List<ReceivingItemResponse> items
) {
}
