package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreatePromotionRequest(
    @NotNull Integer storeId,
    @NotBlank String name,
    String description,
    @NotBlank String promotionType,
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime endsAt,
    Boolean active
) {
}
