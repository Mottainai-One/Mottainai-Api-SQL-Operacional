package com.institutojf.mottainai.security;

import java.util.List;

/** Canonical API permissions derived from the active employee role in the database. */
public final class StaffRoleAuthorities {

    private StaffRoleAuthorities() {
    }

    /** Stable mobile/IA labels; never infer permissions from a display name. */
    public static String mobileRoleForPermissionLevel(Integer permissionLevel) {
        if (permissionLevel == null) return null;
        return switch (permissionLevel) {
            case 100 -> "dono";
            case 80 -> "gerente";
            case 60, 40 -> "estoquista";
            default -> null;
        };
    }

    /** Permission bands follow the operational installer seed levels; role names are display labels. */
    public static List<String> forPermissionLevel(Integer permissionLevel) {
        if (permissionLevel == null) return List.of();
        return switch (permissionLevel) {
            case 100 -> List.of("ADMINISTRATOR", "MANAGER");
            case 80 -> List.of("MANAGER");
            case 60 -> List.of("SUPERVISOR");
            case 40 -> List.of("OPERATOR");
            case 20 -> List.of("INTERN");
            default -> List.of();
        };
    }
}
