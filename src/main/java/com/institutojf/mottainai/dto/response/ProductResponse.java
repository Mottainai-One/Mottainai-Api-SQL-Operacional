package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Integer id,
        Integer companyId,
        Integer categoryId,
        String categoryName,
        Integer taxProfileId,
        String sku,
        String barcode,
        String ncm,
        String cest,
        String name,
        String description,
        String brand,
        String unitMeasure,
        BigDecimal weight,
        Boolean active,
        Integer version
) {
}
