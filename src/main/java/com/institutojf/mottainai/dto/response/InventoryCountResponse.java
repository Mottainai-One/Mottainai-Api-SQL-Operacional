package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.enums.InventoryCountStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record InventoryCountResponse(
        Long id,
        Integer storeId,
        Integer employeeId,
        InventoryCountStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String observation,
        List<InventoryCountItemResponse> items
) {
}
