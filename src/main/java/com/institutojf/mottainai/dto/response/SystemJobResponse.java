package com.institutojf.mottainai.dto.response;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record SystemJobResponse(
        Long id,
        String name,
        String type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationSeconds,
        Integer recordsProcessed,
        Boolean success,
        JsonNode details
) {
}
