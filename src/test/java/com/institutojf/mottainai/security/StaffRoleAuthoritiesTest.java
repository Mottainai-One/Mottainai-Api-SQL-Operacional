package com.institutojf.mottainai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StaffRoleAuthoritiesTest {

    @Test
    void mapsOnlySeededPermissionLevelsToCanonicalAuthorities() {
        assertEquals(List.of("ADMINISTRATOR", "MANAGER"), StaffRoleAuthorities.forPermissionLevel(100));
        assertEquals(List.of("MANAGER"), StaffRoleAuthorities.forPermissionLevel(80));
        assertEquals(List.of("SUPERVISOR"), StaffRoleAuthorities.forPermissionLevel(60));
        assertEquals(List.of("OPERATOR"), StaffRoleAuthorities.forPermissionLevel(40));
        assertEquals(List.of("INTERN"), StaffRoleAuthorities.forPermissionLevel(20));
        assertEquals(List.of(), StaffRoleAuthorities.forPermissionLevel(10));
        assertEquals(List.of(), StaffRoleAuthorities.forPermissionLevel(30));
    }
}
