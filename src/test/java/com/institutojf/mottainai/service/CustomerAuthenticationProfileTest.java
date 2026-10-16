package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.security.CustomerAccess;
import com.institutojf.mottainai.repository.CustomerRepository;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.security.ProtectedDataToken;
import com.institutojf.mottainai.security.TokenHashService;
import com.institutojf.mottainai.security.JwtService;
import com.institutojf.mottainai.security.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
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
import static org.mockito.Mockito.mock;

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

    @Test
    @DisplayName("Should resolve the raw Firebase principal through its protected database UID")
    void shouldResolveFirebaseProfileUsingProtectedUid() {
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerAuthRepository accounts = mock(CustomerAuthRepository.class);
        CustomerAccess access = new CustomerAccess(customers, accounts);
        CustomerAuthenticationService service = new CustomerAuthenticationService(accounts, access, customerService,
                mock(PasswordEncoder.class), new TokenHashService(), mock(JwtService.class), mock(JwtProperties.class),
                mock(CustomerRecoveryDeliveryService.class));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("firebase-uid", null);
        authentication.setAuthenticated(true);
        Customer customer = new Customer();
        customer.setId(7);
        customer.setExternalAuthUid(ProtectedDataToken.externalAuthUid("firebase-uid"));
        when(customers.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(customer.getExternalAuthUid()))
            .thenReturn(Optional.of(customer));
        when(customerService.find(7)).thenReturn(CustomerResponse.fromEntity(customer));

        assertEquals(7, service.profile(authentication).id());

        verify(customers).findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(
                ProtectedDataToken.externalAuthUid("firebase-uid"));
        verifyNoInteractions(accounts);
    }

}
