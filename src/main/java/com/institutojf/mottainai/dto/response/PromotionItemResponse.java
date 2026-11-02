package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.PromotionItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionItemResponse(
        Integer id,
        Integer promotionId,
        Integer productId,
        String productName,
        BigDecimal originalPrice,
        BigDecimal promotionalPrice,
        BigDecimal quantityAvailable,
        LocalDateTime createdAt
) {
    public static PromotionItemResponse fromEntity(PromotionItem item) {
        return new PromotionItemResponse(item.getId(), item.getPromotion().getId(), item.getProduct().getId(),
                item.getProduct().getName(), item.getOriginalPrice(), item.getPromotionalPrice(),
                item.getQuantityAvailable(), item.getCreatedAt());
    }
}
