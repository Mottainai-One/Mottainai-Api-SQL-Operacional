package com.institutojf.mottainai.security;

import java.util.Locale;
import java.util.List;
import java.util.Map;

public final class FirebaseStaffRole {
    private static final Map<String, List<String>> AUTHORITIES = Map.of(
            "dono", List.of("ADMINISTRATOR", "MANAGER"),
            "gerente", List.of("MANAGER"),
            "estoquista", List.of("OPERATOR"));

    private FirebaseStaffRole() {
    }

    public static List<String> authoritiesFor(String role) {
        return role == null ? List.of() : AUTHORITIES.getOrDefault(role.trim().toLowerCase(Locale.ROOT), List.of());
    }
}
