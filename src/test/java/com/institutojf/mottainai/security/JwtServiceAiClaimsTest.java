package com.institutojf.mottainai.security;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.RetailStore;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceAiClaimsTest {

    @Test
    void accessTokenCarriesVerifiedOperationalIdentityForIa() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("signed-token").header("alg", "HS256").claim("sub", "test").build());
        AppUser user = user(41, 7, 3, 40);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "firebase-uid-41", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
        JwtService service = new JwtService(encoder, new JwtProperties("unused", "https://mottainai.local", 60, 7));

        service.generateAccessToken(authentication, UUID.randomUUID(), user);

        var captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        org.mockito.Mockito.verify(encoder).encode(captor.capture());
        var claims = captor.getValue().getClaims();
        assertEquals("firebase-uid-41", claims.getSubject());
        assertEquals(Integer.valueOf(41), claims.getClaim("usuario_id"));
        assertEquals(Integer.valueOf(7), claims.getClaim("empresa_id"));
        assertEquals(Integer.valueOf(3), claims.getClaim("store_id"));
        assertEquals("ESTOQUISTA", claims.getClaim("role"));
        assertEquals("access", claims.getClaim("use"));
        assertNotNull(claims.getId());
        assertNotNull(claims.getExpiresAt());
    }

    @Test
    void ownerTokenKeepsCompanyScopeInsteadOfPinningTheAssignedStore() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("signed-token").header("alg", "HS256").claim("sub", "test").build());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "firebase-owner-uid", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        JwtService service = new JwtService(encoder, new JwtProperties("unused", "https://mottainai.local", 60, 7));
        service.generateAccessToken(authentication, UUID.randomUUID(), user(52, 7, 5, 100));

        var captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        org.mockito.Mockito.verify(encoder).encode(captor.capture());
        var claims = captor.getValue().getClaims();
        assertEquals("DONO", claims.getClaim("role"));
        assertEquals(Integer.valueOf(7), claims.getClaim("empresa_id"));
        org.junit.jupiter.api.Assertions.assertNull(claims.getClaim("store_id"));
    }

    private AppUser user(int userId, int companyId, int storeId, int permissionLevel) {
        AppUser user = mock(AppUser.class);
        Employee employee = mock(Employee.class);
        RetailStore store = mock(RetailStore.class);
        Company company = mock(Company.class);
        EmployeeRole role = mock(EmployeeRole.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmployee()).thenReturn(employee);
        when(employee.getStore()).thenReturn(store);
        when(employee.getRole()).thenReturn(role);
        when(store.getCompany()).thenReturn(company);
        when(store.getId()).thenReturn(storeId);
        when(company.getId()).thenReturn(companyId);
        when(role.getPermissionLevel()).thenReturn(permissionLevel);
        return user;
    }
}
