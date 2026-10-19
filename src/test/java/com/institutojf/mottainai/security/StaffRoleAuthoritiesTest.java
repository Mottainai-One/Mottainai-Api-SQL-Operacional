package com.institutojf.mottainai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StaffRoleAuthoritiesTest {

    @Test
    void mapsEveryPr34DatabaseRoleToItsCanonicalAuthorities() {
        assertEquals(List.of("ADMINISTRATOR", "MANAGER"), StaffRoleAuthorities.forDatabaseRole("Administrator"));
        assertEquals(List.of("MANAGER"), StaffRoleAuthorities.forDatabaseRole("Manager"));
        assertEquals(List.of("SUPERVISOR"), StaffRoleAuthorities.forDatabaseRole("Supervisor"));
        assertEquals(List.of("OPERATOR"), StaffRoleAuthorities.forDatabaseRole("Operator"));
        assertEquals(List.of("INTERN"), StaffRoleAuthorities.forDatabaseRole("Intern"));
        assertEquals(List.of(), StaffRoleAuthorities.forDatabaseRole("Unrecognized role"));
    }
}
