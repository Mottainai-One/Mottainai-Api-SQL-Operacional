package com.institutojf.mottainai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Called only after the Firebase decoder has verified signature, issuer, audience and expiry. */
@Service
@RequiredArgsConstructor
public class FirebaseStaffAuthenticationService {
    private final RlsContextService rlsContextService;
    private final MobileProfileService mobileProfileService;

    @Transactional(readOnly = true)
    public JwtAuthenticationToken authenticate(Jwt jwt) {
        if (!rlsContextService.bootstrapByFirebaseUid(jwt.getSubject())) {
            throw new BadCredentialsException("No active operational assignment for Firebase identity");
        }
        try {
            var profile = mobileProfileService.me(jwt.getSubject());
            var authorities = profile.permissions().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            throw new BadCredentialsException("Operational assignment is unavailable", exception);
        }
    }
}
