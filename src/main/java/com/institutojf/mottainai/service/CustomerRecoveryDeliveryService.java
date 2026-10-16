package com.institutojf.mottainai.service;

import com.institutojf.mottainai.repository.CustomerAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Delivers only committed, current recovery tokens; failed deliveries invalidate their token. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRecoveryDeliveryService {

    private final CustomerAuthRepository customerAuthRepository;

    private final PasswordResetEmailService passwordResetEmailService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendRecoveryLink(String email, String rawToken, String hash, Integer accountId) {
        customerAuthRepository.findByIdForUpdate(accountId)
            .filter(auth -> hash.equals(auth.getRecoveryTokenHash()) && Boolean.TRUE.equals(auth.getActive())
                    && Boolean.TRUE.equals(auth.getCustomer().getActive())
                    && auth.getCustomer().getDeletedAt() == null
                    && auth.getRecoveryExpiresAt() != null
                    && auth.getRecoveryExpiresAt().isAfter(LocalDateTime.now()))
            .ifPresent(auth -> {
                try {
                    passwordResetEmailService.sendRecoveryLink(email, rawToken);
                }
                catch (MailException exception) {
                    auth.setRecoveryTokenHash(null);
                    auth.setRecoveryExpiresAt(null);
                    auth.setUpdatedAt(LocalDateTime.now());
                    customerAuthRepository.saveAndFlush(auth);
                    log.warn("Customer recovery delivery failed for account {} ({})", auth.getId(),
                            exception.getClass().getSimpleName());
                }
            });
    }
}
