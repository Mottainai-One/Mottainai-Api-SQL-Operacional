package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        Integer id,
        String fullName,
        String cpf,
        String email,
        String phone,
        Short birthYear,
        Boolean active,
        Boolean marketingConsent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(customer.getId(), null, null, null, null, customer.getBirthYear(),
                customer.getActive(), customer.getMarketingConsent(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
