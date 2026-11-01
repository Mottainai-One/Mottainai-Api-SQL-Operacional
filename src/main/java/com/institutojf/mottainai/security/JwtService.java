package com.institutojf.mottainai.security;

import com.institutojf.mottainai.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Authentication authentication, UUID sessionId, AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);
        return generateToken(authentication, sessionId, issuedAt, expiresAt, "access", user);
    }

    /** Customer tokens do not carry staff identity claims consumed by the IA layer. */
    public String generateAccessToken(Authentication authentication, UUID sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);
        return generateToken(authentication, sessionId, issuedAt, expiresAt, "access", null);
    }

    public String generateRefreshToken(Authentication authentication, UUID sessionId, long expirationDays,
            AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expirationDays, ChronoUnit.DAYS);
        return generateToken(authentication, sessionId, issuedAt, expiresAt, "refresh", user);
    }

    private String generateToken(Authentication authentication, UUID sessionId, Instant issuedAt, Instant expiresAt,
            String use, AppUser user) {
        List<String> roles = authentication.getAuthorities()
            .stream()
            .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
            .toList();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject(authentication.getName())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("roles", roles)
            // Vincula o token à sessão rotacionável e revogável persistida no banco
            .claim("sid", sessionId.toString())
            .claim("use", use)
            .id(UUID.randomUUID().toString());
        if (user != null) {
            // Inter-service identity contract consumed by Mottainai IA.
            // Keep the Firebase UID in `sub`; numeric IDs are explicit claims.
            claimsBuilder.claim("usuario_id", user.getId())
                .claim("empresa_id", user.getEmployee().getStore().getCompany().getId());
            String aiRole = aiRole(user);
            if (aiRole != null) {
                claimsBuilder.claim("role", aiRole);
                // Owners can query all stores in their company; staff are store scoped.
                if (!"DONO".equals(aiRole)) {
                    claimsBuilder.claim("store_id", user.getEmployee().getStore().getId());
                }
            }
        }
        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String aiRole(AppUser user) {
        Integer permissionLevel = user.getEmployee().getRole().getPermissionLevel();
        if (permissionLevel == null) return null;
        return switch (permissionLevel) {
            case 100 -> "DONO";
            case 80 -> "GERENTE";
            case 40 -> "ESTOQUISTA";
            default -> null;
        };
    }

}
