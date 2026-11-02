package com.institutojf.mottainai.security;

import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerAccess {

    private final CustomerRepository customerRepository;

    private final CustomerAuthRepository customerAuthRepository;

    public void checkAccess(Authentication authentication, Integer customerId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Customer cannot access this record");
        }
        boolean staff = authentication.getAuthorities()
            .stream()
            .anyMatch(authority -> "ROLE_ADMINISTRATOR".equals(authority.getAuthority())
                    || "ROLE_MANAGER".equals(authority.getAuthority()));
        if (!staff && !canAccess(authentication, customerId)) {
            throw new AccessDeniedException("Customer cannot access this record");
        }
    }

    public Customer currentCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        boolean localCustomer = authentication.getAuthorities()
            .stream()
            .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        if (localCustomer) {
            return customerAuthRepository.findByLoginEmailIgnoreCase(authentication.getName())
                .filter(auth -> Boolean.TRUE.equals(auth.getActive()))
                .map(CustomerAuth::getCustomer)
                .filter(customer -> Boolean.TRUE.equals(customer.getActive()) && customer.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }
        return customerRepository.findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(
                ProtectedDataToken.externalAuthUid(authentication.getName()))
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    public boolean canAccess(Authentication authentication, Integer customerId) {
        try {
            return currentCustomer(authentication).getId().equals(customerId);
        }
        catch (ResourceNotFoundException exception) {
            return false;
        }
    }

}
