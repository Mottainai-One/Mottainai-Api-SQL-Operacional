package com.institutojf.mottainai.security;

import com.institutojf.mottainai.service.RlsContextService;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.time.ZoneId;

/**
 * Revalida o JWT interno contra a conta e a sessão antes de liberar a requisição.
 */
public class StaffAccessFilter extends OncePerRequestFilter {

    private final RlsContextService rlsContextService;

    private final CustomerAuthRepository customerAuthRepository;

    private final String apiIssuer;

    public StaffAccessFilter(RlsContextService rlsContextService, CustomerAuthRepository customerAuthRepository,
            JwtProperties jwtProperties) {
        this.rlsContextService = rlsContextService;
        this.customerAuthRepository = customerAuthRepository;
        this.apiIssuer = jwtProperties.issuer();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Bloqueia tokens de refresh, sessões revogadas e permissões desatualizadas
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            Set<String> authorities = token.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
            boolean customerOnly = authorities.equals(Set.of("ROLE_CUSTOMER"));
            boolean apiToken = token.getToken().getIssuer() != null
                    && apiIssuer.equals(token.getToken().getIssuer().toString());
            if (!apiToken) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            boolean valid = "access".equals(token.getToken().getClaimAsString("use"))
                    && (customerOnly ? validCustomerToken(token)
                            : rlsContextService.validateAccessToken(token.getToken()));
            if (!valid) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        // Prossegue com a autenticação validada pelo banco
        filterChain.doFilter(request, response);
    }

    private boolean validCustomerToken(JwtAuthenticationToken token) {
        return customerAuthRepository.findByLoginEmailIgnoreCase(token.getName())
            .filter(auth -> Boolean.TRUE.equals(auth.getActive()))
            .filter(auth -> Boolean.TRUE.equals(auth.getCustomer().getActive()))
            .filter(auth -> auth.getCustomer().getDeletedAt() == null)
            .filter(auth -> token.getToken().getIssuedAt() != null)
            .filter(auth -> !token.getToken()
                .getIssuedAt()
                .isBefore(auth.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant()))
            .isPresent();
    }

}
