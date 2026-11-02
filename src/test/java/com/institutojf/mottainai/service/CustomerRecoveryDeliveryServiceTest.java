package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerRecoveryDeliveryServiceTest {

    @Mock private CustomerAuthRepository repository;
    @Mock private PasswordResetEmailService emailService;
    @InjectMocks private CustomerRecoveryDeliveryService service;

    @Test
    void sendsCurrentTokenAndPreservesItsHash() {
        CustomerAuth auth = currentToken();
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        service.sendRecoveryLink("ana@example.com", "raw-token", "hash", 1);
        verify(emailService).sendRecoveryLink("ana@example.com", "raw-token");
        assertEquals("hash", auth.getRecoveryTokenHash());
    }

    @Test
    void smtpFailureClearsTokenWithoutPropagatingAccountSpecificError() {
        CustomerAuth auth = currentToken();
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        doThrow(new MailSendException("smtp unavailable")).when(emailService)
            .sendRecoveryLink("ana@example.com", "raw-token");
        assertDoesNotThrow(() -> service.sendRecoveryLink("ana@example.com", "raw-token", "hash", 1));
        assertNull(auth.getRecoveryTokenHash());
        assertNull(auth.getRecoveryExpiresAt());
        verify(repository).saveAndFlush(auth);
    }

    @Test
    void supersededTokenDoesNotSendOrInvalidateNewToken() {
        CustomerAuth auth = currentToken();
        auth.setRecoveryTokenHash("new-hash");
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        service.sendRecoveryLink("ana@example.com", "old-raw-token", "old-hash", 1);
        verifyNoInteractions(emailService);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void expiredTokenDoesNotSend() {
        CustomerAuth auth = currentToken();
        auth.setRecoveryExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findByIdForUpdate(1)).thenReturn(Optional.of(auth));
        service.sendRecoveryLink("ana@example.com", "raw-token", "hash", 1);
        verifyNoInteractions(emailService);
    }

    private CustomerAuth currentToken() {
        Customer customer = new Customer();
        customer.setActive(true);
        CustomerAuth auth = new CustomerAuth();
        auth.setId(1);
        auth.setCustomer(customer);
        auth.setRecoveryTokenHash("hash");
        auth.setRecoveryExpiresAt(LocalDateTime.now().plusMinutes(15));
        return auth;
    }
}
