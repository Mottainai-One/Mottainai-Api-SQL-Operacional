package com.institutojf.mottainai.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.junit.jupiter.api.Assertions.*;

class FirebaseJwtDecoderFactoryTest {
    private HttpServer server;
    private RSAKey key;
    private String uri;

    @BeforeEach void setUp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/keys", exchange -> {
            byte[] bytes = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        uri = "http://127.0.0.1:" + server.getAddress().getPort() + "/keys";
    }

    @AfterEach void stop() { server.stop(0); }

    @Test void verifiesRealRsaSignatureAndPreservesFirebaseSubject() throws Exception {
        var decoder = FirebaseJwtDecoderFactory.create("project", uri, Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals("verified-uid", decoder.decode(token(key, "project", "project", Instant.now().plusSeconds(120))).getSubject());
    }

    @Test void refusesWrongSignatureIssuerAudienceAndExpiredTokens() throws Exception {
        var decoder = FirebaseJwtDecoderFactory.create("project", uri, Duration.ofSeconds(1), Duration.ofSeconds(1));
        RSAKey foreign = new RSAKeyGenerator(2048).keyID("test-key").generate();
        assertThrows(JwtException.class, () -> decoder.decode(token(foreign,"project","project",Instant.now().plusSeconds(120))));
        assertThrows(JwtException.class, () -> decoder.decode(token(key,"other","project",Instant.now().plusSeconds(120))));
        assertThrows(JwtException.class, () -> decoder.decode(token(key,"project","other",Instant.now().plusSeconds(120))));
        assertThrows(JwtException.class, () -> decoder.decode(token(key,"project","project",Instant.now().minusSeconds(120))));
    }

    @Test void boundsKeyServerWaitAndNeverAcceptsAnUnverifiedToken() throws Exception {
        server.createContext("/slow", exchange -> {
            try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        String slow = uri.replace("/keys", "/slow");
        var decoder = FirebaseJwtDecoderFactory.create("project", slow, Duration.ofMillis(150), Duration.ofMillis(150));
        assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                assertThrows(JwtException.class, () -> decoder.decode(token(key,"project","project",Instant.now().plusSeconds(120)))));
    }

    private String token(RSAKey signingKey,String issuerProject,String audience,Instant expires) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject("verified-uid")
                .issuer("https://securetoken.google.com/" + issuerProject).audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(300))).expirationTime(Date.from(expires)).build();
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").type(JOSEObjectType.JWT).build(), claims);
        token.sign(new RSASSASigner(signingKey)); return token.serialize();
    }
}
