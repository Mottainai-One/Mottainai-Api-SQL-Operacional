package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateCustomerRequest;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.repository.AddressRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.CustomerRepository;
import com.institutojf.mottainai.repository.LoyaltyAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private CustomerService service;

    @Test
    void createsOperationalCustomerAndLoyaltyAccount() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Ana", "12345678909", "ana@example.com", null, null, true, null);
        when(customerRepository.save(any())).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(7);
            return customer;
        });

        assertEquals(7, service.create(request).id());
        verify(loyaltyAccountRepository).save(any());
    }
}
