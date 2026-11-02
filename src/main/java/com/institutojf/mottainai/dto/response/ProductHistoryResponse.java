package com.institutojf.mottainai.dto.response;

import java.time.LocalDateTime;

public record ProductHistoryResponse(
        Long id,
        Integer productId,
        String fieldName,
        String oldValue,
        String newValue,
        Integer changedBy,
        LocalDateTime changedAt
) {
}
