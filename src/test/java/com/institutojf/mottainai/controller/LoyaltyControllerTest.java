package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.security.CustomerAccess;
import com.institutojf.mottainai.service.LoyaltyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyControllerTest {

    @Mock
    private LoyaltyService loyaltyService;

    @Mock
    private CustomerAccess customerAccess;

    @InjectMocks
    private LoyaltyController controller;

    @Test
    @DisplayName("Should get count for authenticated customer")
    void shouldGetCountForAuthenticatedCustomer() {
        Customer customer = customer(5);
        LoyaltyAccountResponse response = new LoyaltyAccountResponse(10);
        TestingAuthenticationToken authentication = authentication();
        when(customerAccess.currentCustomer(authentication)).thenReturn(customer);
        when(loyaltyService.getLoyaltyAccount(5)).thenReturn(response);
        assertEquals(response, controller.getBalance(authentication));
    }

    private Customer customer(Integer id) {
        Customer customer = new Customer();
        customer.setId(id);
        return customer;
    }

    private TestingAuthenticationToken authentication() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("firebase-uid", null);
        authentication.setAuthenticated(true);
        return authentication;
    }
}