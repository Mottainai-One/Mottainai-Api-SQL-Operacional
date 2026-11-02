package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateDamageReportRequest(
        @NotNull Integer productId,
        @NotBlank @Size(max = 100) String reason,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @Size(max = 500) String note,
        @Pattern(regexp = "^https://res\\.cloudinary\\.com/[^/]+/image/upload/.{1,1900}$") String photoUrl) { }
