package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.InviteStoreUserRequest;
import com.institutojf.mottainai.dto.request.UpdateStoreUserRequest;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.EmployeeRoleRepository;
import com.institutojf.mottainai.mapper.RetailStoreMapper;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.security.ProtectedDataToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    AppUserRepository appUserRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    EmployeeRoleRepository employeeRoleRepository;

    @Mock
    AuditLogRepository auditLogRepository;

    @Mock
    RetailStoreMapper retailStoreMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService service;

    @Test
    @DisplayName("Should return authenticated user with nullable firebase uid")
    void shouldReturnAuthenticatedUserWithNullableFirebaseUid() {
        AppUser user = user(1, "ADMINISTRATOR", true);
        when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@test.com")).thenReturn(Optional.of(user));
        var response = service.me("admin@test.com");
        assertEquals("admin@test.com", response.email());
        assertEquals("***", response.cpf());
        assertEquals("ADMINISTRATOR", response.role());
    }

    @Test
    @DisplayName("Should create inactive invite for authenticated users store")
    void shouldCreateInactiveInviteForAuthenticatedUsersStore() {
        AppUser requester = user(1, "ADMINISTRATOR", true);
        EmployeeRole role = role("OPERATOR");
        when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@test.com"))
            .thenReturn(Optional.of(requester));
        when(appUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("new@test.com")).thenReturn(false);
        String cpfToken = ProtectedDataToken.cpf("12345678901");
        when(employeeRepository.existsByCpf(cpfToken)).thenReturn(false);
        when(employeeRoleRepository.findByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull("OPERATOR"))
            .thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hash");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(10);
            return employee;
        });
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(20);
            return user;
        });
        var response = service.invite(
                new InviteStoreUserRequest(" New User ", "12345678901", " New@Test.com ", " 555-0100 ", "OPERATOR"),
                "admin@test.com");
        assertFalse(response.active());
        assertTrue(response.passwordSetupRequired());
        assertFalse(response.emailNotificationSent());
        assertEquals("New User", response.user().name());
        assertEquals("new@test.com", response.user().email());
        assertEquals("555-0100", response.user().phone());
        assertEquals("***", response.user().cpf());
        verify(passwordEncoder).encode(anyString());
        verify(appUserRepository).save(argThat(
                user -> !user.getActive() && user.getEmployee().getStore() == requester.getEmployee().getStore()
                        && user.getEmail().equals("new@test.com")
                        && user.getEmployee().getEmail().equals("new@test.com")
                        && user.getCpf().equals(cpfToken)
                        && user.getEmployee().getCpf().equals(cpfToken)
                        && user.getEmployee().getName().equals("New User")
                        && user.getEmployee().getPhone().equals("555-0100")));
        verify(auditLogRepository).record(eq("employee"), eq("INSERT"), eq("10"), eq(1), isNull(),
                argThat(data -> data instanceof java.util.Map<?, ?> values
                        && "***".equals(values.get("cpf_masked"))
                        && !values.containsValue("12345678901")
                        && !values.containsValue(cpfToken)));
        verify(auditLogRepository).record(eq("app_user"), eq("INSERT"), eq("20"), eq(1), isNull(),
                argThat(data -> data instanceof java.util.Map<?, ?> values
                        && "***".equals(values.get("cpf_masked"))
                        && !values.containsValue("12345678901")
                        && !values.containsValue(cpfToken)));
    }

    @Test
    @DisplayName("Should reject existing email before creating invite")
    void shouldRejectExistingEmailBeforeCreatingInvite() {
        AppUser requester = user(1, "ADMINISTRATOR", true);
        when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@test.com"))
            .thenReturn(Optional.of(requester));
        when(appUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("new@test.com")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.invite(
                        new InviteStoreUserRequest("New User", "12345678901", "new@test.com", null, "OPERATOR"),
                        "admin@test.com"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update role and status together")
    void shouldUpdateRoleAndStatusTogether() {
        AppUser user = user(2, "OPERATOR", false);
        AppUser requester = user(1, "ADMINISTRATOR", true);
        EmployeeRole role = role("SUPERVISOR");
        when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@test.com"))
            .thenReturn(Optional.of(requester));
        when(appUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(employeeRoleRepository.findByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull("SUPERVISOR"))
            .thenReturn(Optional.of(role));
        when(appUserRepository.save(user)).thenReturn(user);
        var response = service.update(2, new UpdateStoreUserRequest("SUPERVISOR", true), "admin@test.com");
        assertEquals("SUPERVISOR", response.role());
        assertTrue(response.active());
        assertTrue(user.getEmployee().getActive());
        verify(auditLogRepository).record(eq("employee"), eq("UPDATE"), eq("102"), eq(1), any(), any());
        verify(auditLogRepository).record(eq("app_user"), eq("UPDATE"), eq("2"), eq(1), any(), any());
    }

    private AppUser user(int id, String roleName, boolean active) {
        RetailStore store = new RetailStore();
        store.setId(7);
        Employee employee = new Employee();
        employee.setId(id + 100);
        employee.setStore(store);
        employee.setName("User");
        employee.setCpf(ProtectedDataToken.cpf("12345678901"));
        employee.setActive(active);
        employee.setRole(role(roleName));
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail(id == 1 ? "admin@test.com" : "user@test.com");
        user.setCpf(ProtectedDataToken.cpf("12345678901"));
        user.setEmployee(employee);
        user.setActive(active);
        return user;
    }

    private EmployeeRole role(String name) {
        EmployeeRole role = new EmployeeRole();
        role.setId(3);
        role.setName(name);
        role.setActive(true);
        return role;
    }

}
