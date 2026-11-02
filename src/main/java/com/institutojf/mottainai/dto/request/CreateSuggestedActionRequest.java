package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.PriorityLevel;
import com.institutojf.mottainai.model.enums.SuggestedActionType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSuggestedActionRequest(
        @NotNull SuggestedActionType actionType,
        String description,
        @NotNull PriorityLevel priority,
        @NotNull UUID sourceRecommendationUuid
) {
}
