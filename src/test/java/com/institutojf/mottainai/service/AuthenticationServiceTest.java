package com.institutojf.mottainai.service;

import com.institutojf.mottainai.config.PasswordProperties;
import com.institutojf.mottainai.dto.request.ChangePasswordRequest;
import com.institutojf.mottainai.dto.request.ForgotPasswordRequest;
import com.institutojf.mottainai.dto.request.LoginRequest;
import com.institutojf.mottainai.dto.request.ResetPasswordRequest;
import com.institutojf.mottainai.dto.response.TokenResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.PasswordResetToken;
import com.institutojf.mottainai.model.enums.PasswordTokenType;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.EmployeeInvitationTokenRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.PasswordResetTokenRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.JwtProperties;
import com.institutojf.mottainai.security.JwtService;
import com.institutojf.mottainai.security.ProtectedDataToken;
import com.institutojf.mottainai.security.TokenHashService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmployeeInvitationTokenRepository invitationTokenRepository;

    @Mock
    private StaffSessionRepository staffSessionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    @Mock
    private StaffEmailFailureService emailFailureService;

    @Mock
    private RlsContextService rlsContextService;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authenticationManager, appUserRepository, employeeRepository,
                passwordResetTokenRepository, invitationTokenRepository, staffSessionRepository, auditLogRepository,
                passwordResetEmailService, emailFailureService, rlsContextService, tokenHashService, jwtService,
                jwtDecoder, jwtProperties, new PasswordProperties("mottainai,2026"), passwordEncoder, entityManager);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Should return access and refresh tokens for cpf login")
    void shouldReturnAccessAndRefreshTokensForCpfLogin() {
        LoginRequest request = new LoginRequest("12345678901", "password");
        AppUser user = user("manager@mottainai.com");
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        role.setActive(true);
        Employee employee = new Employee();
        employee.setActive(true);
        employee.setCpf(ProtectedDataToken.cpf(request.cpf()));
        employee.setRole(role);
        user.setEmployee(employee);
        user.setActive(true);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(user.getEmail(), null, "ROLE_MANAGER");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(user.getEmail()))
            .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateRefreshToken(any(), any(UUID.class), eq(7L))).thenReturn("refresh-token");
        when(jwtService.generateAccessToken(any(), any(UUID.class))).thenReturn("access-token");
        when(tokenHashService.hash("refresh-token")).thenReturn("refresh-hash");
        when(jwtProperties.expirationMinutes()).thenReturn(60L);
        when(jwtProperties.refreshExpirationDays()).thenReturn(7L);

        TokenResponse response = authenticationService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresIn());
        verify(staffSessionRepository).create(any(UUID.class), eq(user.getId()), eq("refresh-hash"), any());
        verify(entityManager).refresh(user, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        verify(appUserRepository).updateLastLogin(eq(user.getId()), any());
        verify(appUserRepository, never()).save(user);
        verify(authenticationManager).authenticate(argThat(candidate -> candidate instanceof UsernamePasswordAuthenticationToken && request.cpf().equals(candidate.getPrincipal())));
    }

    @Test
    @DisplayName("Should reject CPF login when password changes before the account lock")
    void shouldRejectLoginWhenPasswordChangesBeforeAccountLock() {
        LoginRequest request = new LoginRequest("12345678901", "old-password");
        AppUser user = user("manager@mottainai.com");
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        role.setActive(true);
        Employee employee = new Employee();
        employee.setActive(true);
        employee.setCpf(ProtectedDataToken.cpf(request.cpf()));
        employee.setRole(role);
        user.setEmployee(employee);
        user.setActive(true);
        when(authenticationManager.authenticate(any()))
            .thenReturn(new TestingAuthenticationToken(user.getEmail(), null, "ROLE_MANAGER"));
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(user.getEmail()))
            .thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request));

        verify(entityManager).refresh(user, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        verify(passwordEncoder).matches(request.password(), "old-hash");
        verify(appUserRepository, never()).updateLastLogin(any(), any());
        verify(staffSessionRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject CPF login when employee CPF changes before the account lock")
    void shouldRejectLoginWhenCpfChangesBeforeAccountLock() {
        LoginRequest request = new LoginRequest("12345678901", "password");
        AppUser user = user("manager@mottainai.com");
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        role.setActive(true);
        Employee employee = new Employee();
        employee.setActive(true);
        employee.setCpf(ProtectedDataToken.cpf(request.cpf()));
        employee.setRole(role);
        user.setEmployee(employee);
        user.setActive(true);
        when(authenticationManager.authenticate(any()))
            .thenReturn(new TestingAuthenticationToken(user.getEmail(), null, "ROLE_MANAGER"));
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(user.getEmail()))
            .thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            if (invocation.getArgument(0) == employee) {
                employee.setCpf(ProtectedDataToken.cpf("98765432100"));
            }
            return null;
        }).when(entityManager).refresh(any(), eq(LockModeType.PESSIMISTIC_WRITE));

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request));

        verify(entityManager).refresh(user, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(appUserRepository, never()).updateLastLogin(any(), any());
        verify(staffSessionRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject invalid refresh token as authentication failure")
    void shouldRejectInvalidRefreshTokenAsAuthenticationFailure() {
        when(jwtDecoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));

        assertThrows(BadCredentialsException.class, () -> authenticationService.refresh("invalid"));

        verify(staffSessionRepository, never()).rotate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject predictable password before accessing reset token")
    void shouldRejectPredictablePasswordBeforeAccessingResetToken() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "Mottainai@2026");

        assertThrows(BusinessException.class, () -> authenticationService.resetPassword(request));
        verify(rlsContextService, never()).bootstrapByToken(any(), anyString());
    }

    @Test
    @DisplayName("Should not issue recovery token when identity changes before user lock")
    void shouldNotIssueRecoveryTokenWhenIdentityChangesBeforeUserLock() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("12345678901", "old@mottainai.com");
        AppUser user = user(request.email());
        Employee employee = new Employee();
        employee.setCpf(ProtectedDataToken.cpf(request.cpf()));
        user.setEmployee(employee);
        when(rlsContextService.bootstrapByCpf(request.cpf())).thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(request.email()))
            .thenReturn(Optional.of(user));
        when(appUserRepository.findByIdWithWriteLock(user.getId())).thenReturn(Optional.of(user));
        when(appUserRepository.matchesCurrentIdentity(user.getId(), request.email(),
                ProtectedDataToken.cpf(request.cpf()), true))
            .thenReturn(false);
        authenticationService.requestPasswordReset(request);
        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetEmailService, never()).sendRecoveryLink(any(), any());
    }

    @Test
    @DisplayName("Should invalidate old recovery tokens when changing password")
    void shouldInvalidateOldRecoveryTokensWhenChangingPassword() {
        AppUser user = user("manager@mottainai.com");
        when(appUserRepository.findActiveByEmailWithWriteLock(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old-Password@42", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("New-Password@42")).thenReturn("new-hash");
        authenticationService.changePassword(new ChangePasswordRequest("Old-Password@42", "New-Password@42"),
                user.getEmail());
        assertEquals("new-hash", user.getPasswordHash());
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(user.getId()), any());
        verify(staffSessionRepository).revokeAllForUser(user.getId());
        var order = inOrder(appUserRepository, passwordEncoder);
        order.verify(appUserRepository).findActiveByEmailWithWriteLock(user.getEmail());
        order.verify(passwordEncoder).matches("Old-Password@42", "old-hash");
    }

    @Test
    @DisplayName("Should reject a stale current password after locking the account")
    void shouldRejectStaleCurrentPasswordAfterLock() {
        AppUser user = user("manager@mottainai.com");
        when(appUserRepository.findActiveByEmailWithWriteLock(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(BusinessException.class, () -> authenticationService
            .changePassword(new ChangePasswordRequest("Stale@Password42", "New-Password@42"), user.getEmail()));
        verify(passwordEncoder).matches("Stale@Password42", "old-hash");
        verify(passwordEncoder, never()).encode(any());
        verify(staffSessionRepository, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("Should rotate the refresh token only when the stored hash matches")
    void shouldRotateRefreshToken() {
        AppUser user = user("manager@mottainai.com");
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        Employee employee = new Employee();
        employee.setRole(role);
        user.setEmployee(employee);

        UUID sessionId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);

        when(jwtDecoder.decode("old-refresh")).thenReturn(jwt);
        when(jwt.getClaimAsString("use")).thenReturn("refresh");
        when(jwt.getSubject()).thenReturn(user.getEmail());
        when(jwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(rlsContextService.bootstrapByEmail(user.getEmail())).thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(user.getEmail()))
            .thenReturn(Optional.of(user));
        when(jwtProperties.refreshExpirationDays()).thenReturn(7L);
        when(jwtProperties.expirationMinutes()).thenReturn(60L);
        when(jwtService.generateRefreshToken(any(), eq(sessionId), eq(7L))).thenReturn("next-refresh");
        when(jwtService.generateAccessToken(any(), eq(sessionId))).thenReturn("next-access");
        when(tokenHashService.hash("old-refresh")).thenReturn("old-hash");
        when(tokenHashService.hash("next-refresh")).thenReturn("next-hash");
        when(staffSessionRepository.rotate(eq(sessionId), eq(1), eq("old-hash"), eq("next-hash"), any()))
            .thenReturn(true);

        TokenResponse response = authenticationService.refresh("old-refresh");

        assertEquals("next-access", response.accessToken());
        assertEquals("next-refresh", response.refreshToken());

        verify(staffSessionRepository).rotate(eq(sessionId), eq(1), eq("old-hash"), eq("next-hash"), any());
    }

    @Test
    @DisplayName("Should reject a refresh token after its stored hash was rotated")
    void shouldRejectReusedRefreshToken() {
        AppUser user = user("manager@mottainai.com");
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        Employee employee = new Employee();
        employee.setRole(role);
        user.setEmployee(employee);
        UUID sessionId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("old-refresh")).thenReturn(jwt);
        when(jwt.getClaimAsString("use")).thenReturn("refresh");
        when(jwt.getSubject()).thenReturn(user.getEmail());
        when(jwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(rlsContextService.bootstrapByEmail(user.getEmail())).thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(user.getEmail()))
            .thenReturn(Optional.of(user));
        when(jwtProperties.refreshExpirationDays()).thenReturn(7L);
        when(jwtService.generateRefreshToken(any(), eq(sessionId), eq(7L))).thenReturn("next-refresh");
        when(tokenHashService.hash("old-refresh")).thenReturn("old-hash");
        when(tokenHashService.hash("next-refresh")).thenReturn("next-hash");
        assertThrows(BusinessException.class, () -> authenticationService.refresh("old-refresh"));
        verify(jwtService, never()).generateAccessToken(any(), any());
    }

    @Test
    @DisplayName("Should revoke the matching session on logout")
    void shouldRevokeSessionOnLogout() {
        AppUser user = user("manager@mottainai.com");
        UUID sessionId = UUID.randomUUID();
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(user.getEmail()))
            .thenReturn(Optional.of(user));
        when(tokenHashService.hash("refresh-token")).thenReturn("refresh-hash");
        when(staffSessionRepository.revoke(sessionId, "refresh-hash", user.getId())).thenReturn(true);
        authenticationService.logout("refresh-token", user.getEmail(), sessionId);
        verify(staffSessionRepository).revoke(sessionId, "refresh-hash", user.getId());
    }

    @Test
    @DisplayName("Should activate an invited employee after locking the account and invitation")
    void shouldActivateInvitationWithLocks() {
        AppUser user = user("invite@mottainai.com");
        Employee employee = new Employee();
        employee.setId(5);
        employee.setActive(false);
        user.setEmployee(employee);
        user.setActive(false);
        Long invitationId = 17L;
        when(tokenHashService.hash("invite-token")).thenReturn("invite-hash");
        when(rlsContextService.bootstrapByToken(eq("invite-hash"), anyString()))
            .thenAnswer(invocation -> PasswordTokenType.EMPLOYEE_INVITATION.name().equals(invocation.getArgument(1)) ? user.getId() : null);
        when(appUserRepository.findByIdWithWriteLock(user.getId())).thenReturn(Optional.of(user));
        when(invitationTokenRepository.findUnusedByHashForUpdate("invite-hash"))
            .thenReturn(Optional.of(new EmployeeInvitationTokenRepository.Invitation(invitationId, user.getId(), OffsetDateTime.now().plusHours(1))));
        when(passwordEncoder.encode("Strong@Password42")).thenReturn("encoded-password");
        authenticationService.resetPassword(new ResetPasswordRequest("invite-token", "Strong@Password42"));
        assertTrue(user.getActive());
        assertTrue(user.getEmployee().getActive());
        assertTrue(user.getPasswordSet());
        assertEquals("encoded-password", user.getPasswordHash());
        verify(invitationTokenRepository).markUsed(invitationId);
        verify(staffSessionRepository).revokeAllForUser(user.getId());
        var order = inOrder(appUserRepository, invitationTokenRepository);
        order.verify(appUserRepository).findByIdWithWriteLock(user.getId());
        order.verify(invitationTokenRepository).findUnusedByHashForUpdate("invite-hash");
    }

    @Test
    @DisplayName("Should consume a recovery token and revoke existing sessions")
    void shouldResetPasswordAndRevokeSessions() {
        AppUser user = user("manager@mottainai.com");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        when(tokenHashService.hash("recovery-token")).thenReturn("recovery-hash");
        when(rlsContextService.bootstrapByToken("recovery-hash", PasswordTokenType.PASSWORD_RESET.name()))
            .thenReturn(user.getId());
        when(appUserRepository.findByIdWithWriteLock(user.getId())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findUnusedByHashForUpdate("recovery-hash", PasswordTokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Strong@Password42")).thenReturn("encoded-password");
        authenticationService.resetPassword(new ResetPasswordRequest("recovery-token", "Strong@Password42"));
        assertEquals("encoded-password", user.getPasswordHash());
        verify(passwordResetTokenRepository).save(token);
        verify(staffSessionRepository).revokeAllForUser(user.getId());
        var order = inOrder(appUserRepository, passwordResetTokenRepository);
        order.verify(appUserRepository).findByIdWithWriteLock(user.getId());
        order.verify(passwordResetTokenRepository)
            .findUnusedByHashForUpdate("recovery-hash", PasswordTokenType.PASSWORD_RESET);
    }

    @Test
    @DisplayName("Should send a recovery link only after the token transaction commits")
    void shouldSendRecoveryLinkAfterCommit() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("12345678901", "User@Mottainai.com");
        AppUser user = user("user@mottainai.com");
        Employee employee = new Employee();
        employee.setCpf(ProtectedDataToken.cpf(request.cpf()));
        user.setEmployee(employee);
        when(rlsContextService.bootstrapByCpf(request.cpf())).thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("user@mottainai.com"))
            .thenReturn(Optional.of(user));
        when(appUserRepository.findByIdWithWriteLock(user.getId())).thenReturn(Optional.of(user));
        when(appUserRepository.matchesCurrentIdentity(user.getId(), "user@mottainai.com",
                ProtectedDataToken.cpf(request.cpf()), true))
            .thenReturn(true);
        when(passwordResetTokenRepository.findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
                user.getId(), PasswordTokenType.PASSWORD_RESET))
            .thenReturn(Optional.empty());
        when(tokenHashService.newToken()).thenReturn("recovery-token");
        when(tokenHashService.hash("recovery-token")).thenReturn("recovery-hash");
        TransactionSynchronizationManager.initSynchronization();
        authenticationService.requestPasswordReset(request);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(passwordResetEmailService, never()).sendRecoveryLink(any(), any());
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(passwordResetEmailService).sendRecoveryLink("user@mottainai.com", "recovery-token");
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setId(1);
        user.setEmail(email);
        user.setPasswordHash("old-hash");
        return user;
    }

}
