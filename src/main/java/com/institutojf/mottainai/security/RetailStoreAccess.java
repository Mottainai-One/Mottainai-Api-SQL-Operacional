package com.institutojf.mottainai.security;

import com.institutojf.mottainai.service.RlsContextService;
import com.institutojf.mottainai.service.StaffUserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("retailStoreAccess")
@RequiredArgsConstructor
public class RetailStoreAccess {

    private final RlsContextService rlsContextService;

    private final StaffUserLookup staffUserLookup;

    @Transactional(readOnly = true)
    public boolean isCurrentUserStore(Integer storeId, Authentication authentication) {
        if (storeId == null || authentication == null || authentication.getName() == null
                || !rlsContextService.bootstrapByFirebaseUid(authentication.getName())) {
            return false;
        }
        return staffUserLookup.findActive(authentication.getName())
            .map(user -> storeId.equals(user.getEmployee().getStore().getId()))
            .orElse(false);
    }

}
