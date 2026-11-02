package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.ReceivingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReceivingStatusRequest(
        @NotNull ReceivingStatus status
) {
}
