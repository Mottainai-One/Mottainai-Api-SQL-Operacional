package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.StaffRoleAuthorities;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RlsContextService {

    private final JdbcTemplate jdbcTemplate;

    private final AppUserRepository appUserRepository;

    private final StaffSessionRepository staffSessionRepository;

    /** Sets the tenant context using a Firebase UID from a verified ID token. */
    public boolean bootstrapByFirebaseUid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return false;
        }
        Boolean initialized = jdbcTemplate.queryForObject(
                "SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                firebaseUid, "FIREBASE_UID");
        return Boolean.TRUE.equals(initialized);
    }

    /** Resolves a pending invitation from its opaque, single-use token hash. */
    public Integer bootstrapByToken(String tokenHash, String tokenType) {
        return jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context_by_token(?, ?)",
                Integer.class, tokenHash, tokenType);
    }

    @Transactional(readOnly = true)
    public boolean validateAccessToken(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("use"))
                || !bootstrapByFirebaseUid(jwt.getSubject())) {
            return false;
        }
        UUID sessionId;
        try {
            sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        }
        catch (RuntimeException exception) {
            return false;
        }
        return appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(jwt.getSubject())
                .filter(user -> isCurrentUser(user, jwt, sessionId))
                .isPresent();
    }

    private boolean isCurrentUser(AppUser user, Jwt jwt, UUID sessionId) {
        if (user.getEmployee() == null || user.getEmployee().getRole() == null
                || !Boolean.TRUE.equals(user.getEmployee().getActive()) || user.getEmployee().getDeletedAt() != null
                || !Boolean.TRUE.equals(user.getEmployee().getRole().getActive())
                || user.getEmployee().getRole().getDeletedAt() != null) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> expectedRoles = StaffRoleAuthorities.forDatabaseRole(user.getEmployee().getRole().getName());
        return roles != null && roles.size() == expectedRoles.size() && !expectedRoles.isEmpty()
                && new HashSet<>(roles).equals(new HashSet<>(expectedRoles))
                && staffSessionRepository.isActive(sessionId, user.getId());
    }
}
