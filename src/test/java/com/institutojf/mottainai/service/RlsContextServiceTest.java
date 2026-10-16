package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.ProtectedDataToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RlsContextServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);

    private final StaffSessionRepository staffSessionRepository = mock(StaffSessionRepository.class);

    private final RlsContextService service = new RlsContextService(jdbcTemplate, appUserRepository,
            staffSessionRepository);

    @Test
    @DisplayName("Should use the restricted database function to bootstrap CPF context")
    void shouldBootstrapCpfThroughDatabaseFunction() {
        when(jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                ProtectedDataToken.cpf("12345678901"), "CPF"))
            .thenReturn(true);
        assertTrue(service.bootstrapByCpf("12345678901"));
        verify(jdbcTemplate).queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class,
                ProtectedDataToken.cpf("12345678901"), "CPF");
    }

    @Test
    @DisplayName("Should reject refresh tokens before accessing a staff session")
    void shouldRejectRefreshTokenAsAccessToken() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("use")).thenReturn("refresh");
        assertFalse(service.validateAccessToken(jwt));
        verifyNoInteractions(jdbcTemplate, appUserRepository, staffSessionRepository);
    }

    @Test
    @DisplayName("Should reject an access token when the employee role changed")
    void shouldRejectOutdatedRole() {
        Jwt jwt = mock(Jwt.class);
        UUID sessionId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(1);
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        role.setActive(true);
        Employee employee = new Employee();
        employee.setRole(role);
        employee.setActive(true);
        user.setEmployee(employee);
        when(jwt.getClaimAsString("use")).thenReturn("access");
        when(jwt.getSubject()).thenReturn(" Manager@Example.com ");
        when(jwt.getClaimAsString("sid")).thenReturn(sessionId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ADMINISTRATOR"));
        when(jdbcTemplate.queryForObject("SELECT mottainai.fn_bootstrap_staff_context(?, ?)", Boolean.class, "manager@example.com", "EMAIL"))
            .thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("manager@example.com"))
            .thenReturn(Optional.of(user));
        assertFalse(service.validateAccessToken(jwt));
        verifyNoInteractions(staffSessionRepository);
    }

}
