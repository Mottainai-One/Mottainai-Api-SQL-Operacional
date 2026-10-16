package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CustomerLoginRequest;
import com.institutojf.mottainai.dto.request.CustomerPasswordRecoveryRequest;
import com.institutojf.mottainai.dto.request.CustomerPasswordResetRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAuthenticationServiceTest {

    private static final String SECRET = "A".repeat(43);
    private static final String TOKEN = "1." + SECRET;

    private static final String EMAIL_TOKEN = ProtectedDataToken.email("ana@example.com");

    @Mock private CustomerAuthRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenHashService tokenHashService;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private CustomerRecoveryDeliveryService recoveryDeliveryService;
    @InjectMocks private CustomerAuthenticationService service;

    @Test
    void logsInActiveCustomerUsingProtectedEmailAndClearsFailedAttempts() {
        CustomerAuth auth = auth();
        auth.setFailedAttempts(2);
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.of(auth));
        when(passwordEncoder.matches("Strong1!", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("token");
        when(jwtProperties.expirationMinutes()).thenReturn(60L);
        var response = service.login(new CustomerLoginRequest(" ANA@example.com ", "Strong1!"));
        assertEquals("token", response.accessToken());
        assertEquals(0, auth.getFailedAttempts());
        assertNull(auth.getLockedUntil());
        verify(repository).save(auth);
    }

    @Test
    void locksCustomerForThirtyMinutesAfterFifthFailure() {
        CustomerAuth auth = auth();
        auth.setFailedAttempts(4);
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.of(auth));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        assertThrows(BadCredentialsException.class,
                () -> service.login(new CustomerLoginRequest("ana@example.com", "wrong")));
        assertEquals(5, auth.getFailedAttempts());
        assertTrue(auth.getLockedUntil().isAfter(LocalDateTime.now().plusMinutes(29)));
        verify(repository).save(auth);
    }

    @Test
    void rejectsInactiveCustomerLogin() {
        CustomerAuth auth = auth();
        auth.getCustomer().setActive(false);
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.of(auth));
        assertThrows(BadCredentialsException.class,
                () -> service.login(new CustomerLoginRequest("ana@example.com", "Strong1!")));
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void storesOnlyRecoveryHashAndSendsToNormalizedRequestEmail() {
        CustomerAuth auth = prepareRecovery();
        auth.setRecoveryTokenHash("previous-hash");
        service.requestPasswordReset(new CustomerPasswordRecoveryRequest(" ANA@example.com "));
        assertEquals("token-hash", auth.getRecoveryTokenHash());
        assertTrue(auth.getRecoveryExpiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(auth.getRecoveryExpiresAt().isBefore(LocalDateTime.now().plusMinutes(16)));
        assertEquals(EMAIL_TOKEN, auth.getLoginEmail());
        InOrder order = inOrder(repository, recoveryDeliveryService);
        order.verify(repository).findByLoginEmailForUpdate(EMAIL_TOKEN);
        order.verify(repository).saveAndFlush(auth);
        order.verify(recoveryDeliveryService).sendRecoveryLink("ana@example.com", TOKEN, "token-hash", 1);
    }

    @Test
    void deliversOnlyAfterTransactionCommit() {
        prepareRecovery();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com"));
            verifyNoInteractions(recoveryDeliveryService);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(recoveryDeliveryService).sendRecoveryLink("ana@example.com", TOKEN, "token-hash", 1);
        }
        finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotDeliverWhenTransactionRollsBack() {
        prepareRecovery();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com"));
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(1));
            verifyNoInteractions(recoveryDeliveryService);
        }
        finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void missingAccountReturnsNormallyWithoutCreatingOrSendingToken() {
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com")));
        verifyNoInteractions(tokenHashService, recoveryDeliveryService);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void inactiveAuthOrCustomerAndDeletedCustomerCannotRequestRecovery() {
        CustomerAuth auth = auth();
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.of(auth));
        auth.setActive(false);
        service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com"));
        auth.setActive(true);
        auth.getCustomer().setActive(false);
        service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com"));
        auth.getCustomer().setActive(true);
        auth.getCustomer().setDeletedAt(LocalDateTime.now());
        service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com"));
        verifyNoInteractions(tokenHashService, recoveryDeliveryService);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deliveryInfrastructureFailureDoesNotExposeAccountExistence() {
        prepareRecovery();
        doThrow(new IllegalStateException("delivery unavailable")).when(recoveryDeliveryService)
            .sendRecoveryLink(anyString(), anyString(), anyString(), anyInt());
        assertDoesNotThrow(() -> service.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com")));
    }

    @Test
    void validatesCurrentTokenButRejectsExpiredAndInactiveAccounts() {
        CustomerAuth auth = auth();
        auth.setRecoveryTokenHash("token-hash");
        auth.setRecoveryExpiresAt(LocalDateTime.now().plusMinutes(15));
        when(passwordEncoder.matches(SECRET, "token-hash")).thenReturn(true);
        when(repository.findById(1)).thenReturn(Optional.of(auth));
        assertTrue(service.validateResetToken(TOKEN));
        auth.getCustomer().setActive(false);
        assertFalse(service.validateResetToken(TOKEN));
        auth.getCustomer().setActive(true);
        auth.setRecoveryExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertFalse(service.validateResetToken(TOKEN));
    }

    @Test
    void resetConsumesTokenAndRejectsReplay() {
        CustomerAuth auth = auth();
        auth.setRecoveryTokenHash("token-hash");
        auth.setRecoveryExpiresAt(LocalDateTime.now().plusMinutes(15));
        auth.setFailedAttempts(5);
        auth.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(passwordEncoder.matches(SECRET, "token-hash")).thenReturn(true);
        when(repository.findByIdForUpdate(1))
            .thenAnswer(invocation -> "token-hash".equals(auth.getRecoveryTokenHash()) ? Optional.of(auth) : Optional.empty());
        when(passwordEncoder.encode("NewStrong1!")).thenReturn("new-password-hash");
        var request = new CustomerPasswordResetRequest(TOKEN, "NewStrong1!");
        service.resetPassword(request);
        assertEquals("new-password-hash", auth.getPasswordHash());
        assertNull(auth.getRecoveryTokenHash());
        assertNull(auth.getRecoveryExpiresAt());
        assertNull(auth.getLockedUntil());
        assertEquals(0, auth.getFailedAttempts());
        assertThrows(BusinessException.class, () -> service.resetPassword(request));
        verify(repository, times(1)).save(auth);
    }

    @Test
    void expiredTokenCannotResetPassword() {
        CustomerAuth auth = auth();
        auth.setRecoveryTokenHash("token-hash");
        auth.setRecoveryExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        assertThrows(BusinessException.class,
                () -> service.resetPassword(new CustomerPasswordResetRequest(TOKEN, "NewStrong1!")));
        verifyNoInteractions(passwordEncoder);
        verify(repository, never()).save(any());
    }

    @Test
    void recoveryUsesBcryptRequiredBySchemaAndRejectsTamperedToken() {
        CustomerAuth auth = auth();
        var bcrypt = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
        var realService = new CustomerAuthenticationService(repository, null, null, bcrypt,
                new TokenHashService(), jwtService, jwtProperties, recoveryDeliveryService);
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.of(auth));
        when(repository.findById(1)).thenReturn(Optional.of(auth));
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        realService.requestPasswordReset(new CustomerPasswordRecoveryRequest("ana@example.com"));
        var tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(recoveryDeliveryService).sendRecoveryLink(eq("ana@example.com"), tokenCaptor.capture(),
                eq(auth.getRecoveryTokenHash()), eq(1));
        String rawToken = tokenCaptor.getValue();
        assertTrue(auth.getRecoveryTokenHash().matches("[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}"));
        assertTrue(bcrypt.matches(rawToken.substring(rawToken.indexOf('.') + 1), auth.getRecoveryTokenHash()));
        assertTrue(realService.validateResetToken(rawToken));
        assertFalse(realService.validateResetToken("1." + "B".repeat(43)));
        assertFalse(realService.validateResetToken("malformed"));
        assertFalse(realService.validateResetToken("9999999999." + "B".repeat(43)));
        assertThrows(BusinessException.class, () -> realService.resetPassword(
                new CustomerPasswordResetRequest("1." + "B".repeat(43), "NewStrong1!")));
        realService.resetPassword(new CustomerPasswordResetRequest(rawToken, "NewStrong1!"));
        assertTrue(bcrypt.matches("NewStrong1!", auth.getPasswordHash()));
        assertFalse(realService.validateResetToken(rawToken));
        assertThrows(BusinessException.class,
                () -> realService.resetPassword(new CustomerPasswordResetRequest(rawToken, "NewStrong1!")));
    }

    private CustomerAuth prepareRecovery() {
        CustomerAuth auth = auth();
        when(repository.findByLoginEmailForUpdate(EMAIL_TOKEN)).thenReturn(Optional.of(auth));
        when(tokenHashService.newToken()).thenReturn(SECRET);
        when(passwordEncoder.encode(SECRET)).thenReturn("token-hash");
        return auth;
    }

    private CustomerAuth auth() {
        Customer customer = new Customer();
        customer.setId(1);
        customer.setActive(true);
        CustomerAuth auth = new CustomerAuth();
        auth.setId(1);
        auth.setCustomer(customer);
        auth.setLoginEmail(EMAIL_TOKEN);
        auth.setPasswordHash("hash");
        auth.setActive(true);
        return auth;
    }
}
