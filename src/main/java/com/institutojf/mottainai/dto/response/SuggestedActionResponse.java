package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.enums.PriorityLevel;
import com.institutojf.mottainai.model.SuggestedAction;
import com.institutojf.mottainai.model.enums.SuggestedActionStatus;
import com.institutojf.mottainai.model.enums.SuggestedActionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuggestedActionResponse(
        Integer id,
        Integer alertId,
        SuggestedActionType actionType,
        String description,
        PriorityLevel priority,
        SuggestedActionStatus status,
        UUID sourceRecommendationUuid,
        LocalDateTime generatedAt,
        LocalDateTime createdAt
) {
    public static SuggestedActionResponse fromEntity(SuggestedAction action) {
        return new SuggestedActionResponse(action.getId(), action.getAlert().getId(), action.getActionType(),
                action.getDescription(), action.getPriority(), action.getStatus(), action.getSourceRecommendationUuid(),
                action.getGeneratedAt(), action.getCreatedAt());
    }
}
