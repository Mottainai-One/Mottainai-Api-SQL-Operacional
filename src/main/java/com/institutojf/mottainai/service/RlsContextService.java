package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.FirebaseStaffRole;
import com.institutojf.mottainai.security.ProtectedDataToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RlsContextService {

    private final JdbcTemplate jdbcTemplate;

    private final AppUserRepository appUserRepository;

    private final StaffSessionRepository staffSessionRepository;

    // Inicializa o contexto da transação para o login por CPF
    public boolean bootstrapByCpf(String cpf) {
        return bootstrap(ProtectedDataToken.cpf(cpf), "CPF");
    }

    // Permite consultar um convite pendente antes da ativação da conta
    public boolean bootstrapInvitedByCpf(String cpf) {
        return bootstrap(ProtectedDataToken.cpf(cpf), "INVITED_CPF");
    }

    // Reconstitui o contexto RLS do funcionário pelo email do JWT
    public boolean bootstrapByEmail(String email) {
        return bootstrap(StaffContactData.email(email), "EMAIL");
    }

    // Localiza o dono de um token válido e define o contexto RLS
    public Integer bootstrapByToken(String tokenHash, String tokenType) {
        return jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context_by_token(?, ?)", Integer.class, tokenHash, tokenType);
    }

    /**
     * Compara o papel do JWT com o papel atual do usuário e verifica a sessão Quando a
     * senha muda ou a sessão é revogada, tokens antigos deixam de funcionar
     */
    @Transactional(readOnly = true)
    public boolean validateAccessToken(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("use")) || !bootstrapByEmail(jwt.getSubject())) {
            return false;
        }
        UUID sessionId;
        try {
            sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        }
        catch (RuntimeException exception) {
            return false;
        }
        return appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
                StaffContactData.email(jwt.getSubject()))
            .filter(user -> isCurrentUser(user, jwt, sessionId))
            .isPresent();
    }

    @Transactional
    public boolean validateFirebaseStaffToken(Jwt jwt) {
        String firebaseUid = jwt.getSubject();
        String email = StaffContactData.email(jwt.getClaimAsString("email"));
        if (firebaseUid == null || firebaseUid.isBlank() || email == null || email.isBlank()
                || !Boolean.TRUE.equals(jwt.getClaim("email_verified"))
                || FirebaseStaffRole.authoritiesFor(jwt.getClaimAsString("role")).isEmpty()
                || !bootstrapByEmail(email)) {
            return false;
        }

        AppUser user = appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(email)
                .orElse(null);
        if (user == null || user.getEmployee() == null || !Boolean.TRUE.equals(user.getEmployee().getActive())
                || user.getEmployee().getDeletedAt() != null || user.getEmployee().getRole() == null
                || !Boolean.TRUE.equals(user.getEmployee().getRole().getActive())
                || user.getEmployee().getRole().getDeletedAt() != null
                || !Boolean.TRUE.equals(user.getEmployee().getStore().getActive())
                || user.getEmployee().getStore().getDeletedAt() != null
                || !Boolean.TRUE.equals(user.getEmployee().getStore().getCompany().getActive())
                || user.getEmployee().getStore().getCompany().getDeletedAt() != null) {
            return false;
        }

        if (user.getFirebaseUid() != null) {
            return user.getFirebaseUid().equals(firebaseUid);
        }
        if (appUserRepository.existsByFirebaseUid(firebaseUid)) {
            return false;
        }
        int linked = appUserRepository.linkFirebaseUidIfAbsent(user.getId(), firebaseUid);
        if (linked == 1) {
            user.setFirebaseUid(firebaseUid);
            return true;
        }

        // Another request may have completed the same first-time link concurrently.
        return appUserRepository.existsByIdAndFirebaseUid(user.getId(), firebaseUid);
    }

    // A função restrita do banco valida a identidade e define empresa e loja
    private boolean bootstrap(String identity, String lookup) {
        Boolean initialized = jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)",
                Boolean.class, identity, lookup);
        return Boolean.TRUE.equals(initialized);
    }

    private boolean isCurrentUser(AppUser user, Jwt jwt, UUID sessionId) {
        if (user.getEmployee() == null || user.getEmployee().getRole() == null
                || !Boolean.TRUE.equals(user.getEmployee().getActive()) || user.getEmployee().getDeletedAt() != null
                || !Boolean.TRUE.equals(user.getEmployee().getRole().getActive())
                || user.getEmployee().getRole().getDeletedAt() != null) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        String currentRole = user.getEmployee().getRole().getName().toUpperCase(Locale.ROOT);
        return roles != null && roles.size() == 1 && roles.contains(currentRole)
                && staffSessionRepository.isActive(sessionId, user.getId());
    }

}
