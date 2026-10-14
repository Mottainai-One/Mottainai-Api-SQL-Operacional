package com.institutojf.mottainai.security;

import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryAccess {

    private final AppUserRepository appUserRepository;

    private final RetailStoreRepository retailStoreRepository;

    public AppUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new BusinessException("Authenticated user required");
        }
        return appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public AppUser user(Authentication authentication) {
        return currentUser(authentication);
    }

    public Integer store(Authentication authentication, Integer requestedStoreId) {
        return resolveStoreId(authentication, requestedStoreId);
    }

    public Integer resolveStoreId(Authentication authentication, Integer requestedStoreId) {
        AppUser user = currentUser(authentication);
        Integer userStoreId = user.getEmployee().getStore().getId();
        if (hasRole(authentication, "ADMINISTRATOR")) {
            if (requestedStoreId == null) {
                throw new BusinessException("Store id is required for administrators");
            }
            Integer companyId = user.getEmployee().getStore().getCompany().getId();
            if (!retailStoreRepository.existsByIdAndCompany_IdAndActiveTrueAndDeletedAtIsNull(requestedStoreId,
                    companyId)) {
                throw new BusinessException("User cannot access this store");
            }
            return requestedStoreId;
        }
        return userStoreId;
    }

    public void checkStoreAccess(Authentication authentication, Integer storeId) {
        Integer permittedStoreId = resolveStoreId(authentication, storeId);
        if (!permittedStoreId.equals(storeId)) {
            throw new BusinessException("User cannot access this store");
        }
    }

    public void requireAdministrator(Authentication authentication) {
        currentUser(authentication);
        if (!hasRole(authentication, "ADMINISTRATOR")) {
            throw new BusinessException("Administrator access is required");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

}
