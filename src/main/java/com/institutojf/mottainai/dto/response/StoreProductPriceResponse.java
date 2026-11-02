package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StoreProductPriceResponse(
        Long id,
        Integer storeId,
        Integer productId,
        BigDecimal regularPrice,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Boolean active,
        Integer version
) {
}
