package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.MobileProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseStaffAuthenticationServiceTest {
    private final RlsContextService rls = mock(RlsContextService.class);
    private final MobileProfileService profiles = mock(MobileProfileService.class);
    private final FirebaseStaffAuthenticationService service = new FirebaseStaffAuthenticationService(rls, profiles);
    private final Jwt jwt = Jwt.withTokenValue("verified-firebase-token").header("alg", "RS256")
            .subject("uid").claim("roles", List.of("ADMINISTRATOR")).claim("storeID", "foreign-store").build();

    @Test void usesDatabasePermissionsIgnoringFirebaseCustomClaims() {
        when(rls.bootstrapByFirebaseUid("uid")).thenReturn(true);
        when(profiles.me("uid")).thenReturn(new MobileProfileResponse(1,"uid",null,null,
                "estoquista",true,4,4,List.of("OPERATOR")));
        var auth = service.authenticate(jwt);
        assertEquals("uid", auth.getName());
        assertEquals(List.of("ROLE_OPERATOR"), auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        var order = inOrder(rls, profiles);
        order.verify(rls).bootstrapByFirebaseUid("uid");
        order.verify(profiles).me("uid");
    }

    @Test void refusesUnlinkedUidWithoutReadingProtectedDataOrCreatingAccounts() {
        assertThrows(BadCredentialsException.class, () -> service.authenticate(jwt));
        verifyNoInteractions(profiles);
    }

    @Test void rechecksAssignmentOnEveryRequestAndRejectsDeactivatedAccount() {
        when(rls.bootstrapByFirebaseUid("uid")).thenReturn(true);
        when(profiles.me("uid")).thenThrow(new AccessDeniedException("Inactive account"));
        assertThrows(BadCredentialsException.class, () -> service.authenticate(jwt));
        assertThrows(BadCredentialsException.class, () -> service.authenticate(jwt));
        verify(profiles,times(2)).me("uid");
    }
}
