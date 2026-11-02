package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.AlertType;
import com.institutojf.mottainai.model.enums.PriorityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAlertRequest(
    @NotNull Integer storeId,
    @NotBlank String title,
    String description,
    @NotNull AlertType alertType,
    @NotNull PriorityLevel priority
) {
}
