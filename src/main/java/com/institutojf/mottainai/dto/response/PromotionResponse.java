package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.enums.PromotionStatus;

import java.time.LocalDateTime;

public record PromotionResponse(
        Integer id,
        Integer storeId,
        String name,
        String description,
        String promotionType,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        PromotionStatus status,
        Boolean active,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PromotionResponse fromEntity(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getStore().getId(), promotion.getName(),
                promotion.getDescription(), promotion.getPromotionType(), promotion.getStartsAt(),
                promotion.getEndsAt(), promotion.getStatus(), promotion.getActive(), promotion.getApprovedAt(),
                promotion.getCreatedAt(), promotion.getUpdatedAt());
    }
}
