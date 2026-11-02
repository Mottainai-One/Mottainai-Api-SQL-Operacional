package com.institutojf.mottainai.dto.response;

public record ExecuteSuggestedActionResponse(
        SuggestedActionResponse suggestedAction,
        String entityType,
        Integer entityId
) {
}
