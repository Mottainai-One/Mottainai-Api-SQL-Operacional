package com.institutojf.mottainai.security;

import com.institutojf.mottainai.service.RlsContextService;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerAuth;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAccessFilterTest {

    @Mock
    private RlsContextService rlsContextService;

    @Mock
    private CustomerAuthRepository customerAuthRepository;

    @Mock
    private FirebaseProperties firebaseProperties;

    @InjectMocks
    private StaffAccessFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should reject a staff token that fails session validation")
    void shouldRejectInvalidStaffSession() throws Exception {
        Jwt jwt = token();
        SecurityContextHolder.getContext().setAuthentication(staff(jwt));
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, chain);
        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(rlsContextService).validateAccessToken(jwt);
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("Should pass a staff token with a valid session")
    void shouldAllowValidStaffSession() throws Exception {
        Jwt jwt = token();
        SecurityContextHolder.getContext().setAuthentication(staff(jwt));
        when(rlsContextService.validateAccessToken(jwt)).thenReturn(true);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should validate an operational Firebase staff token")
    void shouldAllowValidFirebaseStaffSession() throws Exception {
        when(firebaseProperties.projectId()).thenReturn("mottainai-operational");
        filter = new StaffAccessFilter(rlsContextService, customerAuthRepository, firebaseProperties);
        Jwt jwt = new Jwt("firebase-token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"),
                Map.of("sub", "staff-uid", "email", "operator@mottainai.com",
                        "iss", "https://securetoken.google.com/mottainai-operational"));
        SecurityContextHolder.getContext().setAuthentication(staff(jwt));
        when(rlsContextService.validateFirebaseStaffToken(jwt)).thenReturn(true);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(rlsContextService).validateFirebaseStaffToken(jwt);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow a customer without a staff session")
    void shouldAllowCustomerWithoutStaffSessionBootstrap() throws Exception {
        Jwt jwt = token();
        Customer customer = new Customer();
        customer.setActive(true);
        CustomerAuth customerAuth = new CustomerAuth();
        customerAuth.setActive(true);
        customerAuth.setCustomer(customer);
        customerAuth.setPasswordChangedAt(LocalDateTime.now().minusMinutes(1));
        when(customerAuthRepository.findByLoginEmailIgnoreCase("admin@example.com"))
            .thenReturn(java.util.Optional.of(customerAuth));
        SecurityContextHolder.getContext()
            .setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verifyNoInteractions(rlsContextService);
        verify(chain).doFilter(request, response);
    }

    private JwtAuthenticationToken staff(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
    }

    private Jwt token() {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", "admin@example.com", "use", "access"));
    }

}
