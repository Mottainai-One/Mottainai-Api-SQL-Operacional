package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.TransferStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTransferStatusRequest(
        @NotNull TransferStatus status
) {
}
