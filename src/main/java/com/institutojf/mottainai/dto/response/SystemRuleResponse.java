package com.institutojf.mottainai.dto.response;

import java.time.LocalDateTime;

public record SystemRuleResponse(
        Integer id,
        String category,
        String key,
        String name,
        String value,
        String valueType,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
