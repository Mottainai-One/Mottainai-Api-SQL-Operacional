package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CustomerLoginRequest;
import com.institutojf.mottainai.dto.request.CustomerPasswordRecoveryRequest;
import com.institutojf.mottainai.dto.request.CustomerPasswordResetRequest;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.dto.response.CustomerTokenResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.security.CustomerAccess;
import com.institutojf.mottainai.security.JwtProperties;
import com.institutojf.mottainai.security.JwtService;
import com.institutojf.mottainai.security.ProtectedDataToken;
import com.institutojf.mottainai.security.TokenHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerAuthenticationService {

    private static final int MAX_ATTEMPTS = 5;

    private static final int LOCK_MINUTES = 30;

    private static final int RESET_MINUTES = 15;

    private final CustomerAuthRepository customerAuthRepository;

    private final CustomerAccess customerAccess;

    private final CustomerService customerService;

    private final PasswordEncoder passwordEncoder;

    private final TokenHashService tokenHashService;

    private final JwtService jwtService;

    private final JwtProperties jwtProperties;

    private final CustomerRecoveryDeliveryService recoveryDeliveryService;

    @Transactional(readOnly = true)
    public CustomerResponse profile(Authentication authentication) {
        return customerService.find(customerAccess.currentCustomer(authentication).getId());
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public CustomerTokenResponse login(CustomerLoginRequest request) {
        CustomerAuth auth = customerAuthRepository.findByLoginEmailForUpdate(ProtectedDataToken.email(request.email()))
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(auth.getActive()) || !Boolean.TRUE.equals(auth.getCustomer().getActive())
                || auth.getCustomer().getDeletedAt() != null
                || auth.getLockedUntil() != null && auth.getLockedUntil().isAfter(now)) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!passwordEncoder.matches(request.password(), auth.getPasswordHash())) {
            int attempts = auth.getFailedAttempts() + 1;
            auth.setFailedAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                auth.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            }
            auth.setUpdatedAt(now);
            customerAuthRepository.save(auth);
            throw new BadCredentialsException("Invalid email or password");
        }
        auth.setFailedAttempts(0);
        auth.setLockedUntil(null);
        auth.setLastLoginAt(now);
        auth.setUpdatedAt(now);
        customerAuthRepository.save(auth);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(auth.getLoginEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return new CustomerTokenResponse(jwtService.generateAccessToken(authentication, UUID.randomUUID()), "Bearer",
                jwtProperties.expirationMinutes() * 60);
    }

    @Transactional
    public void requestPasswordReset(CustomerPasswordRecoveryRequest request) {
        customerAuthRepository.findByLoginEmailForUpdate(ProtectedDataToken.email(request.email()))
            .filter(this::isActiveCustomer)
            .ifPresent(auth -> {
                String secret = tokenHashService.newToken();
                String rawToken = auth.getId() + "." + secret;
                String hash = passwordEncoder.encode(secret);
                LocalDateTime now = LocalDateTime.now();
                auth.setRecoveryTokenHash(hash);
                auth.setRecoveryExpiresAt(now.plusMinutes(RESET_MINUTES));
                auth.setUpdatedAt(now);
                customerAuthRepository.saveAndFlush(auth);
                String recipient = request.email().trim().toLowerCase(Locale.ROOT);
                Runnable delivery = () -> {
                    try {
                        recoveryDeliveryService.sendRecoveryLink(recipient, rawToken, hash, auth.getId());
                    }
                    catch (RuntimeException exception) {
                        // Do not expose account existence or personal data in delivery errors.
                        log.error("Customer recovery delivery could not complete for account {} ({})", auth.getId(),
                                exception.getClass().getSimpleName());
                    }
                };
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            delivery.run();
                        }
                    });
                }
                else {
                    delivery.run();
                }
            });
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String rawToken) {
        return parseRecoveryToken(rawToken)
            .flatMap(token -> customerAuthRepository.findById(token.accountId())
                .filter(auth -> matchesRecoveryToken(auth, token.secret())))
            .isPresent();
    }

    @Transactional
    public void resetPassword(CustomerPasswordResetRequest request) {
        validatePassword(request.newPassword());
        CustomerAuth auth = parseRecoveryToken(request.token())
            .flatMap(token -> customerAuthRepository.findByIdForUpdate(token.accountId())
                .filter(item -> matchesRecoveryToken(item, token.secret())))
            .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));
        auth.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        auth.setPasswordChangedAt(LocalDateTime.now());
        auth.setRecoveryTokenHash(null);
        auth.setRecoveryExpiresAt(null);
        auth.setFailedAttempts(0);
        auth.setLockedUntil(null);
        auth.setUpdatedAt(LocalDateTime.now());
        customerAuthRepository.save(auth);
    }

    private boolean matchesRecoveryToken(CustomerAuth auth, String secret) {
        return isActiveCustomer(auth) && auth.getRecoveryTokenHash() != null
                && auth.getRecoveryExpiresAt() != null
                && auth.getRecoveryExpiresAt().isAfter(LocalDateTime.now())
                && passwordEncoder.matches(secret, auth.getRecoveryTokenHash());
    }

    private Optional<RecoveryToken> parseRecoveryToken(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,9}\\.[A-Za-z0-9_-]{43}")) {
            return Optional.empty();
        }
        int separator = value.indexOf('.');
        try {
            return Optional.of(new RecoveryToken(Integer.parseInt(value.substring(0, separator)),
                    value.substring(separator + 1)));
        }
        catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private record RecoveryToken(int accountId, String secret) {
    }

    private boolean isActiveCustomer(CustomerAuth auth) {
        return Boolean.TRUE.equals(auth.getActive()) && Boolean.TRUE.equals(auth.getCustomer().getActive())
                && auth.getCustomer().getDeletedAt() == null;
    }

    private void validatePassword(String password) {
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean number = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        if (password.length() < 8 || !upper || !lower || !number || !special) {
            throw new BusinessException("Password must contain uppercase, lowercase, number and special character");
        }
    }

}
