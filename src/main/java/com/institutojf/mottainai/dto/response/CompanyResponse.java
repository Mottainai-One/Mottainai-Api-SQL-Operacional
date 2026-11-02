package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record CompanyResponse(
        Integer id,
        SubscriptionPlanResponse plan,
        String officialName,
        String tradeName,
        String cnpj,
        String email,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean active
) {
}
