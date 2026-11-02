package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;

public record RetailStoreResponse(
        Integer id,
        CompanyResponse company,
        AddressResponse address,
        String name,
        String cnpj,
        String email,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean active
) {
}
