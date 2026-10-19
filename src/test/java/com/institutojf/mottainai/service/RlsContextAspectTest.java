package com.institutojf.mottainai.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RlsContextAspectTest {

    private final RlsContextService rlsContextService = mock(RlsContextService.class);
    private final RlsContextAspect aspect = new RlsContextAspect(rlsContextService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bootstrapsStaffContextByVerifiedJwtSubject() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(token("firebase-uid-1", "ROLE_ADMINISTRATOR"));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(rlsContextService.bootstrapByFirebaseUid("firebase-uid-1")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.setContext(joinPoint));
        verify(rlsContextService).bootstrapByFirebaseUid("firebase-uid-1");
        verify(joinPoint).proceed();
    }

    @Test
    void rejectsStaffRequestWhenFirebaseUidCannotBootstrap() {
        SecurityContextHolder.getContext().setAuthentication(token("unlinked-uid", "ROLE_ADMINISTRATOR"));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        assertThrows(BadCredentialsException.class, () -> aspect.setContext(joinPoint));
        verify(rlsContextService).bootstrapByFirebaseUid("unlinked-uid");
    }

    @Test
    void skipsStaffBootstrapForCustomerOnlyToken() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(token("customer-uid", "ROLE_CUSTOMER"));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.setContext(joinPoint));
        verifyNoInteractions(rlsContextService);
    }

    private JwtAuthenticationToken token(String uid, String authority) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", uid, "iss", "https://securetoken.google.com/project"));
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(authority)));
    }
}
