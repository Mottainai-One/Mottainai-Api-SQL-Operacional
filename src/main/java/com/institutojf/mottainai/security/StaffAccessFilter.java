package com.institutojf.mottainai.security;

import com.institutojf.mottainai.service.RlsContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Revalida o JWT interno contra a conta e a sessão antes de liberar a requisição.
 */
public class StaffAccessFilter extends OncePerRequestFilter {

    private final RlsContextService rlsContextService;

    private final String firebaseIssuer;

    public StaffAccessFilter(RlsContextService rlsContextService, FirebaseProperties firebaseProperties) {
        this.rlsContextService = rlsContextService;
        this.firebaseIssuer = "https://securetoken.google.com/" + firebaseProperties.projectId();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Bloqueia tokens de refresh, sessões revogadas e permissões desatualizadas
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            boolean firebaseStaff = Objects.equals(firebaseIssuer, token.getToken().getIssuer() == null
                    ? null : token.getToken().getIssuer().toString());
            if (firebaseStaff && Set.of("/api/v1/auth/logout", "/api/v1/auth/password")
                    .contains(request.getServletPath())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            boolean valid = firebaseStaff
                    ? rlsContextService.validateFirebaseStaffToken(token.getToken())
                    : "access".equals(token.getToken().getClaimAsString("use"))
                            && rlsContextService.validateAccessToken(token.getToken());
            if (!valid) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        // Prossegue com a autenticação validada pelo banco
        filterChain.doFilter(request, response);
    }

}
