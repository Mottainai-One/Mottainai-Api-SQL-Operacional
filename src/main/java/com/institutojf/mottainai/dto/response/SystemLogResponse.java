package com.institutojf.mottainai.dto.response;

import java.time.LocalDateTime;

public record SystemLogResponse(
        Long id,
        String source,
        String level,
        String module,
        String message,
        String stackTrace,
        Integer userId,
        String ipAddress,
        LocalDateTime createdAt
) {
}
