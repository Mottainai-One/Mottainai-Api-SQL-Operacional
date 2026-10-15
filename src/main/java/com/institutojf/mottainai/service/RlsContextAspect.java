package com.institutojf.mottainai.service;

import com.institutojf.mottainai.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Define o contexto RLS do funcionário antes dos serviços transacionais.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RlsContextAspect {

    private final RlsContextService rlsContextService;

    private final JwtProperties jwtProperties;

    @Around("within(com.institutojf.mottainai.service..*) "
            + "&& @annotation(org.springframework.transaction.annotation.Transactional) "
            + "&& !within(com.institutojf.mottainai.service.RlsContextService)")
    public Object setContext(ProceedingJoinPoint joinPoint) throws Throwable {
        // Apenas JWTs internos precisam inicializar o contexto de funcionário
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token
                && jwtProperties.issuer().equals(String.valueOf(token.getToken().getIssuer()))
                && !rlsContextService.bootstrapByEmail(token.getName())) {
            throw new BadCredentialsException("Invalid staff context");
        }
        // A transação segue somente depois de validar o contexto de acesso
        return joinPoint.proceed();
    }

}
