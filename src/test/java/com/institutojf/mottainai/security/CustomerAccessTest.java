package com.institutojf.mottainai.security;

import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAccessTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAuthRepository customerAuthRepository;

    @InjectMocks
    private CustomerAccess customerAccess;

    @Test
    @DisplayName("Should resolve active customer from firebase subject")
    void shouldResolveActiveCustomerFromFirebaseSubject() {
        Customer customer = new Customer();
        customer.setId(7);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("firebase-uid", null);
        authentication.setAuthenticated(true);

        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(ProtectedDataToken.externalAuthUid("firebase-uid")))
            .thenReturn(Optional.of(customer));
        Customer result = customerAccess.currentCustomer(authentication);

        assertEquals(7, result.getId());
        verifyNoInteractions(customerAuthRepository);
    }

    @Test
    @DisplayName("Should reject unknown firebase subject")
    void shouldRejectUnknownFirebaseSubject() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("firebase-uid", null);
        authentication.setAuthenticated(true);
        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(ProtectedDataToken.externalAuthUid("firebase-uid")))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> customerAccess.currentCustomer(authentication));
        verifyNoInteractions(customerAuthRepository);
    }

    @Test
    @DisplayName("Should reject Firebase subject matching an unrelated login email")
    void shouldRejectFirebaseSubjectMatchingLoginEmail() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("ana@example.com", null);
        authentication.setAuthenticated(true);
        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(ProtectedDataToken.externalAuthUid("ana@example.com")))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerAccess.currentCustomer(authentication));
        verifyNoInteractions(customerAuthRepository);
    }

    @Test
    @DisplayName("Should resolve local customer from login account instead of contact email")
    void shouldResolveLocalCustomerFromLoginAccount() {
        Customer customer = new Customer();
        customer.setId(7);
        customer.setEmail(ProtectedDataToken.email("contact@example.com"));
        customer.setActive(true);
        CustomerAuth account = new CustomerAuth();
        account.setCustomer(customer);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(ProtectedDataToken.email("login@example.com"), null, "ROLE_CUSTOMER");
        when(customerAuthRepository.findByLoginEmailIgnoreCase(ProtectedDataToken.email("login@example.com")))
            .thenReturn(Optional.of(account));

        assertEquals(7, customerAccess.currentCustomer(authentication).getId());
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should reject inactive local login account")
    void shouldRejectInactiveLocalLoginAccount() {
        CustomerAuth account = new CustomerAuth();
        account.setActive(false);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(ProtectedDataToken.email("login@example.com"), null, "ROLE_CUSTOMER");
        when(customerAuthRepository.findByLoginEmailIgnoreCase(ProtectedDataToken.email("login@example.com")))
            .thenReturn(Optional.of(account));

        assertThrows(ResourceNotFoundException.class, () -> customerAccess.currentCustomer(authentication));
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should allow administrator access without resolving a customer identity")
    void allowsAdministratorAccessWithoutCustomerLookup() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMINISTRATOR");

        assertDoesNotThrow(() -> customerAccess.checkAccess(authentication, 7));
        verifyNoInteractions(customerRepository, customerAuthRepository);
    }

    @Test
    @DisplayName("Should allow manager access without resolving a customer identity")
    void allowsManagerAccessWithoutCustomerLookup() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("manager@example.com", null, "ROLE_MANAGER");

        assertDoesNotThrow(() -> customerAccess.checkAccess(authentication, 7));
        verifyNoInteractions(customerRepository, customerAuthRepository);
    }

    @Test
    @DisplayName("Should reject missing or unauthenticated customer access")
    void rejectsMissingOrUnauthenticatedAccess() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMINISTRATOR");
        authentication.setAuthenticated(false);

        assertThrows(AccessDeniedException.class, () -> customerAccess.checkAccess(null, 7));
        assertThrows(AccessDeniedException.class, () -> customerAccess.checkAccess(authentication, 7));
        verifyNoInteractions(customerRepository, customerAuthRepository);
    }

    @Test
    @DisplayName("Should allow a SQL customer to access only its own record")
    void allowsSqlCustomerAccessToOwnRecord() {
        Customer customer = new Customer();
        customer.setId(7);
        customer.setActive(true);
        CustomerAuth account = new CustomerAuth();
        account.setCustomer(customer);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(ProtectedDataToken.email("login@example.com"), null, "ROLE_CUSTOMER");
        when(customerAuthRepository.findByLoginEmailIgnoreCase(ProtectedDataToken.email("login@example.com"))).thenReturn(Optional.of(account));

        assertDoesNotThrow(() -> customerAccess.checkAccess(authentication, 7));
        assertThrows(AccessDeniedException.class, () -> customerAccess.checkAccess(authentication, 8));
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should allow a Firebase customer to access only its own record")
    void allowsFirebaseCustomerAccessToOwnRecord() {
        Customer customer = new Customer();
        customer.setId(7);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("firebase-uid", null);
        authentication.setAuthenticated(true);
        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(ProtectedDataToken.externalAuthUid("firebase-uid")))
            .thenReturn(Optional.of(customer));

        assertDoesNotThrow(() -> customerAccess.checkAccess(authentication, 7));
        assertThrows(AccessDeniedException.class, () -> customerAccess.checkAccess(authentication, 8));
        verifyNoInteractions(customerAuthRepository);
    }

    @Test
    @DisplayName("Should reject unknown local account without looking up a Firebase UID")
    void shouldRejectUnknownLocalAccountWithoutFirebaseFallback() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(ProtectedDataToken.email("login@example.com"), null, "ROLE_CUSTOMER");
        when(customerAuthRepository.findByLoginEmailIgnoreCase(ProtectedDataToken.email("login@example.com")))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerAccess.currentCustomer(authentication));
        verifyNoInteractions(customerRepository);
    }

}
