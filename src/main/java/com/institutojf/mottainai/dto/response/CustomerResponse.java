package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        Integer id,
        Short birthYear,
        Boolean active,
        Boolean marketingConsent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getBirthYear(),
                customer.getActive(), customer.getMarketingConsent(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
