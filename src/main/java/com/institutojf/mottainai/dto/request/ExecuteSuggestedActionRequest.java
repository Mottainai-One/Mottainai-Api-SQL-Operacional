package com.institutojf.mottainai.dto.request;

import com.institutojf.mottainai.model.enums.PriorityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExecuteSuggestedActionRequest(
        Integer supplierId,
        LocalDate expectedDeliveryDate,
        Integer destinationStoreId,
        String institution,
        String reason,
        String name,
        String description,
        String promotionType,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String observation,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            Integer productId,
            Integer batchId,
            BigDecimal requestedQuantity,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal originalPrice,
            BigDecimal promotionalPrice,
            BigDecimal quantityAvailable,
            PriorityLevel priority
    ) {
    }
}
