package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.security.CustomerAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthenticationProfileTest {

    @Mock
    private CustomerAccess customerAccess;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerAuthenticationService authenticationService;

    @Test
    @DisplayName("Should return the authenticated customer's operational profile")
    void shouldReturnAuthenticatedCustomerProfile() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("firebase-uid", null);
        authentication.setAuthenticated(true);
        Customer customer = new Customer();
        customer.setId(7);
        CustomerResponse response = CustomerResponse.fromEntity(customer);
        when(customerAccess.currentCustomer(authentication)).thenReturn(customer);
        when(customerService.find(7)).thenReturn(response);

        CustomerResponse result = authenticationService.profile(authentication);

        assertEquals(7, result.id());
        verify(customerService).find(7);
    }

    @Test
    @DisplayName("Should reject a profile request without a linked active customer")
    void shouldRejectUnknownCustomerProfile() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("unknown-uid", null);
        when(customerAccess.currentCustomer(authentication)).thenThrow(new ResourceNotFoundException("Customer not found"));

        assertThrows(ResourceNotFoundException.class, () -> authenticationService.profile(authentication));

        verifyNoInteractions(customerService);
    }

}
