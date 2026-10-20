package com.institutojf.mottainai.service;

import com.institutojf.mottainai.config.PasswordProperties;
import com.institutojf.mottainai.dto.request.ChangePasswordRequest;
import com.institutojf.mottainai.dto.request.LoginRequest;
import com.institutojf.mottainai.dto.request.ResetPasswordRequest;
import com.institutojf.mottainai.dto.response.TokenResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.enums.PasswordTokenType;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.EmployeeInvitationTokenRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.PasswordResetTokenRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.JwtProperties;
import com.institutojf.mottainai.security.JwtService;
import com.institutojf.mottainai.security.StaffRoleAuthorities;
import com.institutojf.mottainai.security.TokenHashService;
import com.institutojf.mottainai.security.FirebaseTokenVerifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;

    private final AppUserRepository appUserRepository;

    private final EmployeeRepository employeeRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final EmployeeInvitationTokenRepository invitationTokenRepository;

    private final StaffSessionRepository staffSessionRepository;

    private final AuditLogRepository auditLogRepository;

    private final RlsContextService rlsContextService;

    private final TokenHashService tokenHashService;

    private final JwtService jwtService;

    private final JwtDecoder jwtDecoder;

    private final JwtProperties jwtProperties;

    private final PasswordProperties passwordProperties;

    private final PasswordEncoder passwordEncoder;

    private final FirebaseTokenVerifier firebaseTokenVerifier;

    private final EntityManager entityManager;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String firebaseUid = firebaseTokenVerifier.verifiedUid(request.firebaseIdToken());
        Authentication authentication = authenticationManager
            .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(firebaseUid, request.password()));
        AppUser user = activeUser(firebaseUid);
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new BadCredentialsException("Invalid Firebase identity or password");
        }
        entityManager.refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        if (!firebaseUid.equals(user.getFirebaseUid()) || !Boolean.TRUE.equals(user.getActive())
                || user.getDeletedAt() != null || !Boolean.TRUE.equals(employee.getActive())
                || employee.getDeletedAt() != null || employee.getRole() == null
                || !Boolean.TRUE.equals(employee.getRole().getActive()) || employee.getRole().getDeletedAt() != null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid Firebase identity or password");
        }
        Authentication currentAuthentication = authenticationFor(user);
        appUserRepository.updateLastLogin(user.getId(), LocalDateTime.now(ZoneOffset.UTC));
        UUID sessionId = UUID.randomUUID();
        String refreshToken = jwtService.generateRefreshToken(currentAuthentication, sessionId,
                jwtProperties.refreshExpirationDays());
        staffSessionRepository.create(sessionId, user.getId(), tokenHashService.hash(refreshToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(jwtProperties.refreshExpirationDays()));
        return tokenResponse(currentAuthentication, user, sessionId, refreshToken);
    }

    // Valida o refresh token e troca seu hash na sessão antes de emitir novos tokens
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        Jwt token;
        try {
            token = jwtDecoder.decode(rawRefreshToken);
        }
        catch (JwtException exception) {
            throw new BadCredentialsException("Invalid refresh token", exception);
        }
        if (!"refresh".equals(token.getClaimAsString("use"))
                || !rlsContextService.bootstrapByFirebaseUid(token.getSubject())) {
            throw new BusinessException("Invalid refresh token");
        }
        AppUser user = activeUser(token.getSubject());
        UUID sessionId = sessionId(token);
        Authentication authentication = authenticationFor(user);
        String nextRefreshToken = jwtService.generateRefreshToken(authentication, sessionId,
                jwtProperties.refreshExpirationDays());
        if (!staffSessionRepository.rotate(sessionId, user.getId(), tokenHashService.hash(rawRefreshToken),
                tokenHashService.hash(nextRefreshToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(jwtProperties.refreshExpirationDays()))) {
            throw new BusinessException("Invalid refresh token");
        }
        return tokenResponse(authentication, user, sessionId, nextRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken, String firebaseUid, UUID sessionId) {
        AppUser user = activeUser(firebaseUid);
        if (!staffSessionRepository.revoke(sessionId, tokenHashService.hash(rawRefreshToken), user.getId())) {
            throw new BusinessException("Invalid refresh token");
        }
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String rawToken) {
        String hash = tokenHashService.hash(rawToken);
        return rlsContextService.bootstrapByToken(hash, PasswordTokenType.EMPLOYEE_INVITATION.name()) != null;
    }

    /**
     * Conclui um convite usando o token de convite e um Firebase ID token validado.
     * As duas credenciais são necessárias para vincular a identidade Firebase à conta pendente.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validateNewPassword(request.newPassword());
        String firebaseUid = firebaseTokenVerifier.verifiedUid(request.firebaseIdToken());
        // Busca apenas o hash do token para não persistir o valor recebido por email
        String hash = tokenHashService.hash(request.token());
        Integer userId = rlsContextService.bootstrapByToken(hash, PasswordTokenType.EMPLOYEE_INVITATION.name());
        if (userId == null) {
            throw new BusinessException("Invalid or expired invitation token");
        }
        AppUser user = appUserRepository.findByIdWithWriteLock(userId)
            .orElseThrow(() -> new BusinessException("Invalid or expired invitation token"));
        var invitation = invitationTokenRepository.findUnusedByHashForUpdate(hash)
                .orElseThrow(() -> new BusinessException("Invalid or expired invitation token"));
        if (!invitation.userId().equals(userId)
                || !invitation.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException("Invalid or expired invitation token");
        }
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(firebaseUid)) {
            throw new BusinessException("Invitation is already linked to another Firebase account");
        }
        if (appUserRepository.existsByFirebaseUid(firebaseUid) && !firebaseUid.equals(user.getFirebaseUid())) {
            throw new BusinessException("Firebase account is already linked to another user");
        }
        user.setFirebaseUid(firebaseUid);
        user.getEmployee().setActive(true);
        user.setActive(true);
        user.setPasswordSet(true);
        employeeRepository.save(user.getEmployee());
        invitationTokenRepository.markUsed(invitation.id());
        auditLogRepository.record("employee", "UPDATE", user.getEmployee().getId().toString(), userId,
                Map.of("active", false), Map.of("active", true));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
        passwordResetTokenRepository.invalidateUnusedForUser(userId, OffsetDateTime.now(ZoneOffset.UTC));
        staffSessionRepository.revokeAllForUser(userId);
        auditLogRepository.record("app_user", "UPDATE", userId.toString(), userId, null,
                Map.of("password_changed", true, "activated", true, "firebase_uid_linked", true));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String firebaseUid) {
        AppUser user = appUserRepository.findActiveByFirebaseUidWithWriteLock(firebaseUid)
            .orElseThrow(() -> new BusinessException("User is unavailable"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        validateNewPassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
        passwordResetTokenRepository.invalidateUnusedForUser(user.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        staffSessionRepository.revokeAllForUser(user.getId());
        auditLogRepository.record("app_user", "UPDATE", user.getId().toString(), user.getId(), null,
                Map.of("password_changed", true));
    }

    private TokenResponse tokenResponse(Authentication authentication, AppUser user, UUID sessionId, String refreshToken) {
        return new TokenResponse(jwtService.generateAccessToken(authentication, sessionId), refreshToken, "Bearer",
                jwtProperties.expirationMinutes() * 60, jwtProperties.refreshExpirationDays() * 24 * 60 * 60);
    }

    // Resolve a conta pelo identificador de Firebase já verificado.
    private AppUser activeUser(String firebaseUid) {
        return appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid)
            .orElseThrow(() -> new BusinessException("User is unavailable"));
    }

    // Reconstrói as permissões atuais do funcionário para emitir novos tokens
    private Authentication authenticationFor(AppUser user) {
        List<SimpleGrantedAuthority> authorities = StaffRoleAuthorities
                .forPermissionLevel(user.getEmployee().getRole().getPermissionLevel()).stream()
                .map(authority -> new SimpleGrantedAuthority("ROLE_" + authority))
                .toList();
        if (authorities.isEmpty()) {
            throw new BusinessException("Employee role is not supported for API authentication");
        }
        return UsernamePasswordAuthenticationToken.authenticated(
                user.getFirebaseUid(), null, authorities);
    }

    // Lê o identificador da sessão do JWT e rejeita valores inválidos
    private UUID sessionId(Jwt token) {
        try {
            return UUID.fromString(token.getClaimAsString("sid"));
        }
        catch (RuntimeException exception) {
            throw new BusinessException("Invalid refresh token");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long");
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean number = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars()
            .anyMatch(character -> !Character.isLetterOrDigit(character) && !Character.isWhitespace(character));
        boolean predictable = Arrays.stream(passwordProperties.predictableTerms().split(","))
            .map(String::trim)
            .filter(term -> !term.isEmpty())
            .anyMatch(password.toLowerCase(Locale.ROOT)::contains);
        if (!upper || !lower || !number || !special || predictable) {
            throw new BusinessException(
                    "Password must contain uppercase, lowercase, number and special character and must not contain predictable terms");
        }
    }

}
