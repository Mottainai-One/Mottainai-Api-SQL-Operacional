package com.institutojf.mottainai.dto.response;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemEventResponse(
        Long id,
        UUID uuid,
        String eventType,
        String aggregateType,
        String aggregateId,
        JsonNode data,
        Integer priority,
        String status,
        Integer retryCount,
        String errorMessage,
        LocalDateTime occurredAt,
        LocalDateTime processedAt
) {
}
