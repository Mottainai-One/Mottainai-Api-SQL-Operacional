package com.institutojf.mottainai.security;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Canonical API permissions derived from the active employee role in the database. */
public final class StaffRoleAuthorities {

    private static final Map<String, List<String>> AUTHORITIES_BY_DATABASE_ROLE = Map.of(
            "ADMINISTRATOR", List.of("ADMINISTRATOR", "MANAGER"),
            "MANAGER", List.of("MANAGER"),
            "SUPERVISOR", List.of("SUPERVISOR"),
            "OPERATOR", List.of("OPERATOR"),
            "INTERN", List.of("INTERN"));

    private StaffRoleAuthorities() {
    }

    public static List<String> forDatabaseRole(String roleName) {
        return roleName == null ? List.of()
                : AUTHORITIES_BY_DATABASE_ROLE.getOrDefault(roleName.trim().toUpperCase(Locale.ROOT), List.of());
    }
}
