package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RlsContextServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final StaffSessionRepository staffSessionRepository = mock(StaffSessionRepository.class);
    private final RlsContextService service = new RlsContextService(jdbcTemplate, appUserRepository,
            staffSessionRepository);

    @Test
    @DisplayName("Bootstraps staff context only with a Firebase UID")
    void bootstrapsWithFirebaseUid() {
        when(jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                "firebase-user-123", "FIREBASE_UID")).thenReturn(true);

        assertTrue(service.bootstrapByFirebaseUid("firebase-user-123"));

        verify(jdbcTemplate).queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                "firebase-user-123", "FIREBASE_UID");
    }

    @Test
    @DisplayName("Rejects refresh tokens before accessing a staff session")
    void rejectsRefreshTokenAsAccessToken() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("use")).thenReturn("refresh");

        assertFalse(service.validateAccessToken(jwt));
        verifyNoInteractions(jdbcTemplate, appUserRepository, staffSessionRepository);
    }

    @Test
    @DisplayName("Rejects an access token when the employee role changed")
    void rejectsOutdatedRole() {
        Jwt jwt = mock(Jwt.class);
        UUID sessionId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(1);
        user.setFirebaseUid("firebase-user-123");
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        role.setPermissionLevel(80);
        role.setActive(true);
        Employee employee = new Employee();
        employee.setRole(role);
        employee.setActive(true);
        user.setEmployee(employee);
        when(jwt.getClaimAsString("use")).thenReturn("access");
        when(jwt.getSubject()).thenReturn("firebase-user-123");
        when(jwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ADMINISTRATOR"));
        when(jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                "firebase-user-123", "FIREBASE_UID")).thenReturn(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("firebase-user-123"))
                .thenReturn(Optional.of(user));

        assertFalse(service.validateAccessToken(jwt));
        verifyNoInteractions(staffSessionRepository);
    }

    @Test
    @DisplayName("Does not accept administrator authorities for a manager, even with Firebase owner claim")
    void rejectsAdministratorAuthoritySetForDatabaseManager() {
        Jwt jwt = mock(Jwt.class);
        UUID sessionId = UUID.randomUUID();
        AppUser user = activeUser("firebase-manager-uid", "Manager");
        when(jwt.getClaimAsString("use")).thenReturn("access");
        when(jwt.getSubject()).thenReturn("firebase-manager-uid");
        when(jwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ADMINISTRATOR", "MANAGER"));
        when(jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                "firebase-manager-uid", "FIREBASE_UID")).thenReturn(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("firebase-manager-uid"))
                .thenReturn(Optional.of(user));

        assertFalse(service.validateAccessToken(jwt));
        verifyNoInteractions(staffSessionRepository);
    }

    @Test
    @DisplayName("Accepts the canonical administrator authority matrix")
    void acceptsCanonicalAdministratorAuthorities() {
        Jwt jwt = mock(Jwt.class);
        UUID sessionId = UUID.randomUUID();
        AppUser user = activeUser("firebase-admin-uid", "Administrator");
        when(jwt.getClaimAsString("use")).thenReturn("access");
        when(jwt.getSubject()).thenReturn("firebase-admin-uid");
        when(jwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ADMINISTRATOR", "MANAGER"));
        when(jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                "firebase-admin-uid", "FIREBASE_UID")).thenReturn(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("firebase-admin-uid"))
                .thenReturn(Optional.of(user));
        when(staffSessionRepository.isActive(sessionId, user.getId())).thenReturn(true);

        assertTrue(service.validateAccessToken(jwt));
        verify(staffSessionRepository).isActive(sessionId, user.getId());
    }

    private static AppUser activeUser(String uid, String roleName) {
        AppUser user = new AppUser();
        user.setId(1);
        user.setFirebaseUid(uid);
        EmployeeRole role = new EmployeeRole();
        role.setName(roleName);
        role.setPermissionLevel(switch (roleName) {
            case "Administrator" -> 100;
            case "Manager" -> 80;
            case "Supervisor" -> 60;
            case "Operator" -> 40;
            case "Intern" -> 20;
            default -> -1;
        });
        role.setActive(true);
        Employee employee = new Employee();
        employee.setRole(role);
        employee.setActive(true);
        user.setEmployee(employee);
        user.setActive(true);
        return user;
    }
}
