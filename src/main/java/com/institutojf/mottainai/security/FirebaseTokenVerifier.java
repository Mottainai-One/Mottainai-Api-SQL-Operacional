package com.institutojf.mottainai.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/** Verifies Firebase-signed ID tokens before using their subject as a staff identifier. */
@Component
public class FirebaseTokenVerifier {

    private final JwtDecoder firebaseJwtDecoder;

    public FirebaseTokenVerifier(@Qualifier("firebaseJwtDecoder") JwtDecoder firebaseJwtDecoder) {
        this.firebaseJwtDecoder = firebaseJwtDecoder;
    }

    public String verifiedUid(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BadCredentialsException("A verified Firebase ID token is required");
        }
        try {
            Jwt jwt = firebaseJwtDecoder.decode(idToken);
            String uid = jwt.getSubject();
            if (uid == null || uid.isBlank()) {
                throw new BadCredentialsException("Firebase ID token does not contain a subject");
            }
            return uid;
        }
        catch (JwtException exception) {
            throw new BadCredentialsException("Invalid Firebase ID token", exception);
        }
    }
}
