package com.institutojf.mottainai.security;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.service.RlsContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RlsContextService rlsContextService;

    @Test
    void hashesRawCpfBeforeBootstrapAndRepositoryLookup() {
        String rawCpf = "12345678901";
        String cpfToken = ProtectedDataToken.cpf(rawCpf);
        EmployeeRole role = new EmployeeRole();
        role.setName("OPERATOR");
        role.setActive(true);
        Employee employee = new Employee();
        employee.setCpf(cpfToken);
        employee.setActive(true);
        employee.setRole(role);
        AppUser user = new AppUser();
        user.setEmail("operator@example.com");
        user.setPasswordHash("encoded-password");
        user.setEmployee(employee);

        when(rlsContextService.bootstrapByCpf(cpfToken)).thenReturn(true);
        when(appUserRepository.findByEmployee_CpfAndActiveTrueAndDeletedAtIsNull(cpfToken))
                .thenReturn(Optional.of(user));

        var details = new DatabaseUserDetailsService(appUserRepository, rlsContextService).loadUserByUsername(rawCpf);

        assertEquals("operator@example.com", details.getUsername());
        verify(rlsContextService).bootstrapByCpf(cpfToken);
        verify(appUserRepository).findByEmployee_CpfAndActiveTrueAndDeletedAtIsNull(cpfToken);
    }
}
