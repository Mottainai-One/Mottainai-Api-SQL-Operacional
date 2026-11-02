package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PosApprovalResponse(UUID id, Integer storeId, Integer employeeId, Integer saleId,
        LocalDateTime saleDate, Integer saleItemId, Integer cancellationRequestId, String requestType,
        String reason, BigDecimal requestedAmount, BigDecimal discountPercent, String status,
        Integer decidedByEmployeeId, OffsetDateTime createdAt, OffsetDateTime decidedAt) {
}
