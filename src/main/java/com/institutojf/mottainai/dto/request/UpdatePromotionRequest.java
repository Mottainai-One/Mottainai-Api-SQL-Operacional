package com.institutojf.mottainai.dto.request;

import java.time.LocalDateTime;

public record UpdatePromotionRequest(
        String name,
        String description,
        String promotionType,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active
) {
}
