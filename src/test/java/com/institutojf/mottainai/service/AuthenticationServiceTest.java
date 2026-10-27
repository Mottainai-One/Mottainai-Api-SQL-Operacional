package com.institutojf.mottainai.service;

import com.institutojf.mottainai.config.PasswordProperties;
import com.institutojf.mottainai.dto.request.ChangePasswordRequest;
import com.institutojf.mottainai.dto.request.LoginRequest;
import com.institutojf.mottainai.dto.request.ResetPasswordRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.SubscriptionPlan;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.EmployeeInvitationTokenRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.PasswordResetTokenRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.FirebaseTokenVerifier;
import com.institutojf.mottainai.security.JwtProperties;
import com.institutojf.mottainai.security.JwtService;
import com.institutojf.mottainai.security.TokenHashService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private AppUserRepository appUserRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmployeeInvitationTokenRepository invitationTokenRepository;
    @Mock private StaffSessionRepository staffSessionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private RlsContextService rlsContextService;
    @Mock private TokenHashService tokenHashService;
    @Mock private JwtService jwtService;
    @Mock private JwtDecoder jwtDecoder;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FirebaseTokenVerifier firebaseTokenVerifier;
    @Mock private EntityManager entityManager;

    private AuthenticationService service() {
        return new AuthenticationService(authenticationManager, appUserRepository, employeeRepository,
                passwordResetTokenRepository, invitationTokenRepository, staffSessionRepository, auditLogRepository,
                rlsContextService, tokenHashService, jwtService, jwtDecoder,
                new JwtProperties("secret", "issuer", 60, 7), new PasswordProperties("mottainai,2026"),
                passwordEncoder, firebaseTokenVerifier, entityManager);
    }

    @Test
    void loginUsesVerifiedFirebaseUidAndApiPassword() {
        String uid = "verified-firebase-uid";
        AppUser user = activeUser(uid);
        when(firebaseTokenVerifier.verifiedUid("signed-id-token")).thenReturn(uid);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(uid, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Strong!Pass123", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateRefreshToken(any(), any(UUID.class), eq(7L))).thenReturn("refresh-token");
        when(jwtService.generateAccessToken(any(), any(UUID.class))).thenReturn("access-token");
        when(tokenHashService.hash("refresh-token")).thenReturn("refresh-hash");

        var response = service().login(new LoginRequest("signed-id-token", "Strong!Pass123"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        verify(firebaseTokenVerifier).verifiedUid("signed-id-token");
        verify(appUserRepository).findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(uid);
        verify(entityManager).refresh(user, LockModeType.PESSIMISTIC_WRITE);
        verify(jwtService).generateAccessToken(argThat(auth -> auth.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList().equals(List.of("ROLE_ADMINISTRATOR", "ROLE_MANAGER"))), any(UUID.class));
    }

    @Test
    void loginRejectsPasswordThatNoLongerMatchesAfterLockingUser() {
        String uid = "verified-firebase-uid";
        AppUser user = activeUser(uid);
        when(firebaseTokenVerifier.verifiedUid("signed-id-token")).thenReturn(uid);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(uid, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old!Password123", "bcrypt-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service().login(new LoginRequest("signed-id-token", "Old!Password123")));

        verify(entityManager).refresh(user, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(user.getEmployee(), LockModeType.PESSIMISTIC_WRITE);
        verify(appUserRepository, never()).updateLastLogin(any(), any());
        verify(staffSessionRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    void refreshRotatesOnlyTheMatchingRefreshTokenHash() {
        String uid = "firebase-manager-uid";
        AppUser user = activeUser(uid, "Manager");
        UUID sessionId = UUID.randomUUID();
        var refreshJwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwtDecoder.decode("old-refresh-token")).thenReturn(refreshJwt);
        when(refreshJwt.getClaimAsString("use")).thenReturn("refresh");
        when(refreshJwt.getSubject()).thenReturn(uid);
        when(refreshJwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(rlsContextService.bootstrapByFirebaseUid(uid)).thenReturn(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(jwtService.generateRefreshToken(any(), eq(sessionId), eq(7L))).thenReturn("rotated-refresh-token");
        when(jwtService.generateAccessToken(any(), eq(sessionId))).thenReturn("next-access-token");
        when(tokenHashService.hash("old-refresh-token")).thenReturn("old-hash");
        when(tokenHashService.hash("rotated-refresh-token")).thenReturn("new-hash");
        when(staffSessionRepository.rotate(eq(sessionId), eq(user.getId()), eq("old-hash"), eq("new-hash"), any()))
                .thenReturn(true);

        var response = service().refresh("old-refresh-token");

        assertEquals("next-access-token", response.accessToken());
        assertEquals("rotated-refresh-token", response.refreshToken());
        verify(staffSessionRepository).rotate(eq(sessionId), eq(user.getId()), eq("old-hash"), eq("new-hash"), any());
    }

    @Test
    void refreshRejectsReplayWhenTheStoredHashWasAlreadyRotated() {
        String uid = "firebase-manager-uid";
        AppUser user = activeUser(uid, "Manager");
        UUID sessionId = UUID.randomUUID();
        var refreshJwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwtDecoder.decode("old-refresh-token")).thenReturn(refreshJwt);
        when(refreshJwt.getClaimAsString("use")).thenReturn("refresh");
        when(refreshJwt.getSubject()).thenReturn(uid);
        when(refreshJwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(rlsContextService.bootstrapByFirebaseUid(uid)).thenReturn(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(jwtService.generateRefreshToken(any(), eq(sessionId), eq(7L))).thenReturn("new-refresh-token");
        when(tokenHashService.hash("old-refresh-token")).thenReturn("old-hash");
        when(tokenHashService.hash("new-refresh-token")).thenReturn("new-hash");
        when(staffSessionRepository.rotate(eq(sessionId), eq(user.getId()), eq("old-hash"), eq("new-hash"), any()))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> service().refresh("old-refresh-token"));
        verify(jwtService, never()).generateAccessToken(any(), any());
    }

    @Test
    void logoutRevokesOnlyTheMatchingRefreshSession() {
        String uid = "firebase-staff-uid";
        AppUser user = activeUser(uid);
        UUID sessionId = UUID.randomUUID();
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(tokenHashService.hash("refresh-token")).thenReturn("refresh-hash");
        when(staffSessionRepository.revoke(sessionId, "refresh-hash", user.getId())).thenReturn(true);

        service().logout("refresh-token", uid, sessionId);

        verify(staffSessionRepository).revoke(sessionId, "refresh-hash", user.getId());
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndRevokesExistingSessions() {
        String uid = "firebase-staff-uid";
        AppUser user = activeUser(uid);
        when(appUserRepository.findActiveByFirebaseUidWithWriteLock(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current!Password123", "bcrypt-hash")).thenReturn(true);
        when(passwordEncoder.encode("Updated!Password123")).thenReturn("new-bcrypt-hash");

        service().changePassword(new ChangePasswordRequest("Current!Password123", "Updated!Password123"), uid);

        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(user.getId()), any());
        verify(staffSessionRepository).revokeAllForUser(user.getId());
        verify(appUserRepository).save(user);
    }

    @Test
    void passwordChangeRejectsStaleCurrentPasswordWithoutMutation() {
        String uid = "firebase-staff-uid";
        AppUser user = activeUser(uid);
        when(appUserRepository.findActiveByFirebaseUidWithWriteLock(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Stale!Password123", "bcrypt-hash")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service().changePassword(
                new ChangePasswordRequest("Stale!Password123", "Updated!Password123"), uid));

        verify(passwordEncoder, never()).encode(any());
        verify(staffSessionRepository, never()).revokeAllForUser(any());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void invitationActivationLinksOnlyTheUidFromVerifiedFirebaseToken() {
        String uid = "verified-firebase-uid";
        int userId = 9;
        AppUser user = activeUser(null);
        user.setId(userId);
        user.setActive(false);
        user.getEmployee().setActive(false);
        user.setPasswordSet(false);
        when(firebaseTokenVerifier.verifiedUid("signed-id-token")).thenReturn(uid);
        when(tokenHashService.hash("one-time-invite")).thenReturn("token-hash");
        when(rlsContextService.bootstrapByToken("token-hash", "EMPLOYEE_INVITATION")).thenReturn(userId);
        when(appUserRepository.findByIdWithWriteLock(userId)).thenReturn(Optional.of(user));
        when(invitationTokenRepository.findUnusedByHashForUpdate("token-hash"))
                .thenReturn(Optional.of(new EmployeeInvitationTokenRepository.Invitation(55L, userId,
                        OffsetDateTime.now().plusHours(1))));
        when(appUserRepository.existsByFirebaseUid(uid)).thenReturn(false);
        when(passwordEncoder.encode("Strong!Pass123")).thenReturn("new-bcrypt-hash");

        service().resetPassword(new ResetPasswordRequest("one-time-invite", "signed-id-token", "Strong!Pass123"));

        assertEquals(uid, user.getFirebaseUid());
        assertTrue(user.getActive());
        assertTrue(user.getPasswordSet());
        assertTrue(user.getEmployee().getActive());
        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        verify(invitationTokenRepository).markUsed(55L);
        verify(appUserRepository).save(user);
        verify(staffSessionRepository).revokeAllForUser(userId);
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(userId), any());
        var orderedLocks = inOrder(appUserRepository, invitationTokenRepository);
        orderedLocks.verify(appUserRepository).findByIdWithWriteLock(userId);
        orderedLocks.verify(invitationTokenRepository).findUnusedByHashForUpdate("token-hash");
        verify(auditLogRepository).record(eq("app_user"), eq("UPDATE"), eq("9"), eq(9), eq(null), any(Map.class));
    }

    @Test
    void invalidOrExpiredInvitationDoesNotLinkFirebaseUid() {
        AppUser user = activeUser(null);
        user.setId(9);
        user.setActive(false);
        user.getEmployee().setActive(false);
        when(firebaseTokenVerifier.verifiedUid("signed-id-token")).thenReturn("verified-firebase-uid");
        when(tokenHashService.hash("invalid-token")).thenReturn("token-hash");
        when(rlsContextService.bootstrapByToken("token-hash", "EMPLOYEE_INVITATION")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service().resetPassword(new ResetPasswordRequest("invalid-token", "signed-id-token",
                        "Strong!Pass123")));
        assertEquals(null, user.getFirebaseUid());
    }

    @Test
    void invitationValidationUsesOnlyInvitationTokenBootstrap() {
        when(tokenHashService.hash("invite")).thenReturn("hashed-invite");
        when(rlsContextService.bootstrapByToken("hashed-invite", "EMPLOYEE_INVITATION")).thenReturn(3);

        assertTrue(service().validateResetToken("invite"));
        when(rlsContextService.bootstrapByToken("hashed-invite", "EMPLOYEE_INVITATION")).thenReturn(null);
        assertFalse(service().validateResetToken("invite"));
    }

    private static AppUser activeUser(String uid) {
        return activeUser(uid, "Administrator");
    }

    private static AppUser activeUser(String uid, String roleName) {
        EmployeeRole role = new EmployeeRole();
        role.setId(1);
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
        employee.setId(4);
        employee.setRole(role);
        employee.setActive(true);
        RetailStore store = new RetailStore();
        Company company = new Company();
        company.setId(3);
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setUserLimit(10);
        company.setPlan(plan);
        store.setCompany(company);
        employee.setStore(store);
        AppUser user = new AppUser();
        user.setId(9);
        user.setFirebaseUid(uid);
        user.setEmployee(employee);
        user.setActive(true);
        user.setPasswordHash("bcrypt-hash");
        user.setPasswordSet(true);
        return user;
    }
}
