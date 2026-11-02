package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.DonationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDonationStatusRequest(
        @NotNull DonationStatus status
) {
}
