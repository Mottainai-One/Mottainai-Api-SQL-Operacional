package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.SuggestedActionStatus;
import jakarta.validation.constraints.NotNull;

public record SuggestedActionDecisionRequest(
        @NotNull SuggestedActionStatus decision
) {
}
