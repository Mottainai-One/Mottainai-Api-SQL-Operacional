package com.institutojf.mottainai.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.institutojf.mottainai.model.Customer;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CustomerResponseTest {

    @Test
    void exposesOnlyOperationalCustomerMetadata() {
        Customer customer = new Customer();
        customer.setId(7);
        customer.setBirthYear((short) 1999);
        customer.setActive(true);
        customer.setMarketingConsent(false);
        customer.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        customer.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));

        CustomerResponse response = CustomerResponse.fromEntity(customer);

        assertEquals(7, response.id());
        assertEquals((short) 1999, response.birthYear());
        assertEquals(true, response.active());
        assertEquals(false, response.marketingConsent());
    }
}
