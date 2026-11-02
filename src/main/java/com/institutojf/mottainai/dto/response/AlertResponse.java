package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Alert;
import com.institutojf.mottainai.model.enums.AlertStatus;
import com.institutojf.mottainai.model.enums.AlertType;
import com.institutojf.mottainai.model.enums.PriorityLevel;

import java.time.LocalDateTime;

public record AlertResponse(
    Integer id,
    Integer storeId,
    String title,
    String description,
    AlertType alertType,
    PriorityLevel priority,
    AlertStatus status,
    LocalDateTime generatedAt,
    LocalDateTime resolvedAt,
    LocalDateTime createdAt
) {
    public static AlertResponse fromEntity(Alert alert) {
        return new AlertResponse(
            alert.getId(),
            alert.getStore().getId(),
            alert.getTitle(),
            alert.getDescription(),
            alert.getAlertType(),
            alert.getPriority(),
            alert.getStatus(),
            alert.getGeneratedAt(),
            alert.getResolvedAt(),
            alert.getCreatedAt()
        );
    }
}
