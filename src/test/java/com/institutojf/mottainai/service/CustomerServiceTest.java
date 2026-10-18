package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateCustomerRequest;
import com.institutojf.mottainai.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.dto.request.UpdateCustomerRequest;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.repository.*;
import com.institutojf.mottainai.security.ProtectedDataToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAuthRepository customerAuthRepository;

    @Mock
    private CustomerGeofenceRepository geofenceRepository;

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService service;

    @Test
    @DisplayName("Should create customer authentication and loyalty account atomically")
    void createsCustomerAuthenticationAndLoyaltyAccountAtomically() {
        CreateCustomerRequest request = new CreateCustomerRequest("Ana", "12345678909", "ana@example.com", "Strong1!", null, null, true, null);
        when(customerAuthRepository.findByLoginEmailIgnoreCase(ProtectedDataToken.email(request.email())))
            .thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hash");
        when(customerRepository.save(any())).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(7);
            return customer;
        });
        assertEquals(7, service.create(request).id());
        verify(customerRepository).save(argThat(customer ->
                ProtectedDataToken.customerName(request.fullName()).equals(customer.getFullName())
                    && ProtectedDataToken.cpf(request.cpf()).equals(customer.getCpf())
                    && ProtectedDataToken.email(request.email()).equals(customer.getEmail())
                    && customer.getBirthYear() == null));
        verify(customerAuthRepository).save(argThat(auth -> auth.getCustomer().getId().equals(7)
                && auth.getLoginEmail().equals(ProtectedDataToken.email(request.email()))
                && auth.getPasswordHash().equals("hash") && auth.getActive()));
        verify(loyaltyAccountRepository)
            .save(argThat(account -> account.getPointsBalance() == 0 && account.getCustomer().getId().equals(7)));
    }

    @Test
    void rejectsWeakPasswordBeforeWritingCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest("Ana", "12345678909", "ana@example.com", "weakpass", null, null, false, null);
        assertThrows(BusinessException.class, () -> service.create(request));
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should create Firebase customer profile without storing a password")
    void createsFirebaseCustomerProfileWithoutPassword() {
        var request = new CreateFirebaseCustomerProfileRequest("Ana", "12345678909", null, null, true, null);
        when(customerRepository.findByExternalAuthUid(ProtectedDataToken.externalAuthUid("firebase-uid")))
            .thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(8);
            return customer;
        });

        var response = service.createFirebaseProfile(request, "firebase-uid", "ana@example.com");

        assertEquals(8, response.id());
        verify(customerRepository).save(argThat(customer -> customer.getExternalAuthUid()
                .equals(ProtectedDataToken.externalAuthUid("firebase-uid"))));
        verify(customerAuthRepository, never()).save(any());
        verify(loyaltyAccountRepository).save(argThat(account -> account.getCustomer().getId().equals(8)
                && account.getPointsBalance() == 0));
    }

    @Test
    @DisplayName("Should return an existing profile for a repeated Firebase registration")
    void returnsExistingFirebaseProfileOnRetry() {
        Customer existing = new Customer();
        existing.setId(8);
        existing.setFullName(ProtectedDataToken.customerName("Ana"));
        existing.setEmail(ProtectedDataToken.email("ana@example.com"));
        existing.setActive(true);
        when(customerRepository.findByExternalAuthUid(ProtectedDataToken.externalAuthUid("firebase-uid")))
            .thenReturn(Optional.of(existing));

        var response = service.createFirebaseProfile(
            new CreateFirebaseCustomerProfileRequest("Ignored", "12345678909", null, null, false, null),
            "firebase-uid", "ana@example.com");

        assertEquals(8, response.id());
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(loyaltyAccountRepository);
    }

    @Test
    @DisplayName("Should update editable Firebase customer profile fields without changing identity data")
    void updatesFirebaseCustomerProfile() {
        Customer customer = new Customer();
        customer.setId(8);
        customer.setExternalAuthUid(ProtectedDataToken.externalAuthUid("firebase-uid"));
        customer.setFullName(ProtectedDataToken.customerName("Ana"));
        customer.setEmail(ProtectedDataToken.email("ana@example.com"));
        customer.setCpf(ProtectedDataToken.cpf("12345678909"));
        customer.setMarketingConsent(false);
        customer.setActive(true);
        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(
                ProtectedDataToken.externalAuthUid("firebase-uid")))
            .thenReturn(Optional.of(customer));

        var response = service.updateFirebaseProfile(
                new UpdateFirebaseCustomerProfileRequest("Ana Silva", "11999999999", true), "firebase-uid");

        assertTrue(response.marketingConsent());
        assertEquals(ProtectedDataToken.phone("11999999999"), customer.getPhone());
        assertEquals(ProtectedDataToken.customerName("Ana Silva"), customer.getFullName());
        verify(customerRepository).save(customer);
        verifyNoInteractions(customerAuthRepository);
    }

    @Test
    void preservesProtectedPhoneWhenFirebaseProfileUpdateOmitsIt() {
        Customer customer = new Customer();
        customer.setId(8);
        customer.setFullName(ProtectedDataToken.customerName("Ana"));
        customer.setMarketingConsent(false);
        customer.setPhone(ProtectedDataToken.phone("11999999999"));
        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(
                ProtectedDataToken.externalAuthUid("firebase-uid"))).thenReturn(Optional.of(customer));

        service.updateFirebaseProfile(new UpdateFirebaseCustomerProfileRequest("Ana Silva", null, true),
                "firebase-uid");

        assertEquals(ProtectedDataToken.phone("11999999999"), customer.getPhone());
        verify(customerRepository).save(customer);
    }

    @Test
    void explicitlyBlankPhoneRemovesTheProtectedToken() {
        Customer customer = new Customer();
        customer.setId(8);
        customer.setFullName(ProtectedDataToken.customerName("Ana"));
        customer.setMarketingConsent(false);
        customer.setPhone(ProtectedDataToken.phone("11999999999"));
        when(customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(
                ProtectedDataToken.externalAuthUid("firebase-uid"))).thenReturn(Optional.of(customer));

        service.updateFirebaseProfile(new UpdateFirebaseCustomerProfileRequest("Ana", "", false),
                "firebase-uid");

        assertNull(customer.getPhone());
        verify(customerRepository).save(customer);
    }

    @Test
    void updatesProtectedCustomerWithInitiallyNullEmailAndStoresOnlyBirthYearAndAddressToken() {
        Customer customer = new Customer();
        customer.setId(7);
        customer.setFullName(ProtectedDataToken.customerName("Ana"));
        when(customerRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(customer));
        var address = new CreateAddressRequest("01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP");
        var request = new UpdateCustomerRequest("Ana Silva", " ANA@example.com ", "(11) 99999-9999",
            LocalDate.of(1990, 5, 12), true, address);

        service.update(7, request);

        assertEquals(ProtectedDataToken.customerName("Ana Silva"), customer.getFullName());
        assertEquals(ProtectedDataToken.email("ana@example.com"), customer.getEmail());
        assertEquals(ProtectedDataToken.phone("11999999999"), customer.getPhone());
        assertEquals((short) 1990, customer.getBirthYear());
        assertEquals(ProtectedDataToken.address("01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP"),
            customer.getAddressToken());
        verify(customerRepository).existsByEmailIgnoreCaseAndDeletedAtIsNull(ProtectedDataToken.email("ana@example.com"));
        verify(customerRepository).save(customer);
        verifyNoInteractions(addressRepository);
    }

    @Test
    void anonymizationClearsProtectedIdentifiersAndDisablesLinkedAccounts() {
        Customer customer = new Customer();
        customer.setId(7);
        customer.setEmail(ProtectedDataToken.email("ana@example.com"));
        customer.setCpf(ProtectedDataToken.cpf("12345678909"));
        customer.setPhone(ProtectedDataToken.phone("11999999999"));
        customer.setBirthYear((short) 1990);
        customer.setExternalAuthUid(ProtectedDataToken.externalAuthUid("firebase-uid"));
        CustomerAuth auth = new CustomerAuth();
        auth.setRecoveryTokenHash("old-recovery-hash");
        when(customerRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(customer));
        when(customerAuthRepository.findByCustomer_Id(7)).thenReturn(Optional.of(auth));

        service.anonymize(7);

        assertTrue(customer.getFullName().matches("CUS_SHA256_[A-F0-9]{64}"));
        assertNull(customer.getEmail());
        assertNull(customer.getCpf());
        assertNull(customer.getPhone());
        assertNull(customer.getBirthYear());
        assertNull(customer.getExternalAuthUid());
        assertNull(customer.getAddressToken());
        assertFalse(customer.getActive());
        assertNotNull(customer.getDeletedAt());
        assertFalse(auth.getActive());
        assertNull(auth.getRecoveryTokenHash());
        assertNull(auth.getRecoveryExpiresAt());
        assertTrue(auth.getLoginEmail().matches("EMAIL_SHA256_[A-F0-9]{64}"));
    }

    @Test
    void rejectsBirthYearsOutsideInstallerConstraintBeforePersistence() {
        for (LocalDate birthDate : new LocalDate[] { LocalDate.of(1899, 1, 1), LocalDate.now().plusYears(1) }) {
            var request = new CreateFirebaseCustomerProfileRequest("Ana", "12345678909", null, birthDate, false, null);
            assertThrows(BusinessException.class,
                () -> service.createFirebaseProfile(request, "firebase-uid", "ana@example.com"));
        }
        verify(customerRepository, never()).save(any());
        verifyNoInteractions(loyaltyAccountRepository);
    }

}
