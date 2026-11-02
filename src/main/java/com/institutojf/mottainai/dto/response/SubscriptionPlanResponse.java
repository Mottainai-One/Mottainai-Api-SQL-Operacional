package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record SubscriptionPlanResponse(
        Integer id,
        String name,
        String description,
        BigDecimal price,
        Integer storeLimit,
        Integer userLimit,
        Boolean active
) {
}
