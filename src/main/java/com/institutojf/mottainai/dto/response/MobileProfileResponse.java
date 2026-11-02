package com.institutojf.mottainai.dto.response;

import java.util.List;

/** Identity and authorization resolved exclusively from the operational database. */
public record MobileProfileResponse(Integer id, String firebaseUid, String name, String email,
        String role, Boolean active, Integer storeId, Integer companyId, List<String> permissions) {
}
