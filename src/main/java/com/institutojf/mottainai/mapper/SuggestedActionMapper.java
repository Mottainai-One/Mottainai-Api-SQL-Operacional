package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import com.institutojf.mottainai.model.SuggestedAction;
import org.springframework.stereotype.Component;

@Component
public class SuggestedActionMapper {

    public SuggestedActionResponse toResponse(SuggestedAction action) {
        return new SuggestedActionResponse(action.getId(), action.getAlert().getId(), action.getActionType(),
                action.getDescription(), action.getPriority(), action.getStatus(), action.getSourceRecommendationUuid(),
                action.getGeneratedAt(), action.getCreatedAt());
    }

}
