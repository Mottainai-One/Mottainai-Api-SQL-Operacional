package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAlertStatusRequest(
        @NotNull AlertStatus status
) {
}
