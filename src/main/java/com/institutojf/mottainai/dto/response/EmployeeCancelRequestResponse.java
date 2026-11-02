package com.institutojf.mottainai.dto.response;

import java.time.LocalDateTime;

public record EmployeeCancelRequestResponse(
        Integer cancelRequestId,
        Integer saleId,
        Integer saleItemId,
        String targetType,
        String reason,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt
) {
}
