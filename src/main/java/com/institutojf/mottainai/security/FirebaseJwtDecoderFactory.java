package com.institutojf.mottainai.security;

import java.time.Duration;
import java.io.IOException;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

/** Bounded public-key retrieval; TLS and Firebase token validation remain mandatory. */
final class FirebaseJwtDecoderFactory {
    private FirebaseJwtDecoderFactory() {
    }

    static JwtDecoder create(String projectId, String jwkSetUri, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory transport = new SimpleClientHttpRequestFactory();
        transport.setConnectTimeout(connectTimeout);
        transport.setReadTimeout(readTimeout);
        RestTemplate keys = new RestTemplate(transport);
        // Only this public-key GET uses the retry. Never retry business writes or accept an unverified JWT.
        keys.setInterceptors(List.of((request, body, execution) -> {
            try {
                return execution.execute(request, body);
            } catch (IOException transientFailure) {
                return execution.execute(request, body);
            }
        }));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(keys).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Firebase audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId), audience));
        return decoder;
    }
}
