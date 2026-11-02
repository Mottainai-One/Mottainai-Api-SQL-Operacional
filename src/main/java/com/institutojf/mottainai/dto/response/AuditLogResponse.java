package com.institutojf.mottainai.dto.response;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long auditId,
        String tableAffected,
        String operation,
        String recordId,
        Integer userId,
        JsonNode oldData,
        JsonNode newData,
        LocalDateTime operationDate
) {
}
