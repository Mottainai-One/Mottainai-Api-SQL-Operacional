package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateEmployeeRequest;
import com.institutojf.mottainai.dto.request.EmployeeStatusRequest;
import com.institutojf.mottainai.dto.request.UpdateEmployeeRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.InvitationDeliveryException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.EmployeeActivityRepository;
import com.institutojf.mottainai.repository.EmployeeInvitationTokenRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.EmployeeRoleRepository;
import com.institutojf.mottainai.repository.PasswordResetTokenRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.repository.StaffSessionRepository;
import com.institutojf.mottainai.security.ProtectedDataToken;
import com.institutojf.mottainai.security.TokenHashService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @BeforeEach
    void authenticateAdministrator() {
        authenticateAs("ADMINISTRATOR");
    }

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeRoleRepository roleRepository;

    @Mock
    private RetailStoreRepository storeRepository;

    @Mock
    private EmployeeActivityRepository activityRepository;

    @Mock
    private EmployeeInvitationTokenRepository invitationRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private StaffSessionRepository sessionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordResetEmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private StaffEmailFailureService emailFailureService;

    @Mock
    private EntityManager entityManager;

    @AfterEach
    void clearTransactionSynchronization() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Should not activate employee before password is defined")
    void shouldNotActivateEmployeeBeforePasswordIsDefined() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "invite@example.com", "OPERATOR", false);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("admin@example.com"))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        assertThrows(BusinessException.class,
                () -> service.changeStatus(2, new EmployeeStatusRequest(true), "admin@example.com"));
        verify(employeeRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
        verify(entityManager).refresh(invitedEmployee, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("Should reject employee activation when role or store is inactive")
    void shouldRejectActivationWithInactiveRoleOrStore() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser employee = user(2, "employee@example.com", "OPERATOR", true);
        employee.getEmployee().getStore().setActive(true);
        employee.getEmployee().getRole().setActive(false);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(employee));
        assertThrows(BusinessException.class,
                () -> service.changeStatus(2, new EmployeeStatusRequest(true), administrator.getEmail()));
        employee.getEmployee().getRole().setActive(true);
        employee.getEmployee().getStore().setActive(false);
        assertThrows(BusinessException.class,
                () -> service.changeStatus(2, new EmployeeStatusRequest(true), administrator.getEmail()));
        verify(employeeRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should invalidate recovery and invitation tokens when disabling an employee")
    void shouldInvalidateTokensWhenDisablingEmployee() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser employee = user(2, "employee@example.com", "OPERATOR", true);
        employee.setActive(true);
        employee.getEmployee().setActive(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(employee));
        var response = service.changeStatus(2, new EmployeeStatusRequest(false), administrator.getEmail());
        assertFalse(response.active());
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(2), any(OffsetDateTime.class));
        verify(invitationRepository).invalidateUnusedForUser(2);
        verify(sessionRepository).revokeAllForUser(2);
        verify(entityManager).refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(employee.getEmployee(), LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("Should cancel invitation when disabling an employee without a password")
    void shouldCancelInvitationForInvitedEmployee() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "invite@example.com", "OPERATOR", false);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        service.changeStatus(2, new EmployeeStatusRequest(false), administrator.getEmail());
        verify(invitationRepository).invalidateUnusedForUser(2);
        verify(emailService, never()).sendInvitationLink(any(), any());
        assertFalse(invitedEmployee.getActive());
        assertFalse(invitedEmployee.getEmployee().getActive());
    }

    @Test
    @DisplayName("Should let an administrator issue a new invitation after cancellation")
    void shouldResendInvitationForInactiveEmployeeWithoutPassword() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "invite@example.com", "OPERATOR", false);
        invitedEmployee.getEmployee().getRole().setActive(true);
        invitedEmployee.getEmployee().getStore().setActive(true);
        invitedEmployee.getEmployee().getStore().getCompany().setActive(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        when(tokenHashService.newToken()).thenReturn("new-invite-token");
        when(tokenHashService.hash("new-invite-token")).thenReturn("new-token-hash");
        TransactionSynchronizationManager.initSynchronization();
        service.resendInvitation(2, administrator.getEmail());
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(2), any(OffsetDateTime.class));
        verify(invitationRepository).invalidateUnusedForUser(2);
        verify(invitationRepository).create(eq(2), eq("new-token-hash"), any(OffsetDateTime.class));
        verify(emailService, never()).sendInvitationLink(any(), any());
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(emailService).sendInvitationLink("invite@example.com", "new-invite-token");
    }

    @Test
    @DisplayName("Should invalidate invitation and report email failure after commit")
    void shouldInvalidateInvitationWhenEmailFails() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "invite@example.com", "OPERATOR", false);
        invitedEmployee.getEmployee().getRole().setActive(true);
        invitedEmployee.getEmployee().getStore().setActive(true);
        invitedEmployee.getEmployee().getStore().getCompany().setActive(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        when(tokenHashService.newToken()).thenReturn("new-invite-token");
        when(tokenHashService.hash("new-invite-token")).thenReturn("new-token-hash");
        doThrow(new MailSendException("SMTP unavailable")).when(emailService)
            .sendInvitationLink("invite@example.com", "new-invite-token");
        TransactionSynchronizationManager.initSynchronization();
        service.resendInvitation(2, administrator.getEmail());
        assertThrows(InvitationDeliveryException.class, TransactionSynchronizationUtils::triggerAfterCommit);
        verify(emailFailureService).invalidateInvitationToken("new-token-hash");
        assertFalse(Boolean.TRUE.equals(invitedEmployee.getActive()));
    }

    @Test
    @DisplayName("Should not invite an employee whose password is already set")
    void shouldRejectInvitationForEmployeeWithPassword() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser employee = user(2, "employee@example.com", "OPERATOR", true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(employee));
        assertThrows(BusinessException.class, () -> service.resendInvitation(2, administrator.getEmail()));
        verify(invitationRepository, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Should not issue an invitation when the employee role is inactive")
    void shouldRejectInvitationForInactiveRole() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "invite@example.com", "OPERATOR", false);
        invitedEmployee.getEmployee().getRole().setActive(false);
        invitedEmployee.getEmployee().getStore().setActive(true);
        invitedEmployee.getEmployee().getStore().getCompany().setActive(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        assertThrows(BusinessException.class, () -> service.resendInvitation(2, administrator.getEmail()));
        verify(invitationRepository, never()).invalidateUnusedForUser(2);
        verify(invitationRepository, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Should limit repeated pending invitations")
    void shouldLimitRepeatedInvitation() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "invite@example.com", "OPERATOR", false);
        invitedEmployee.getEmployee().getRole().setActive(true);
        invitedEmployee.getEmployee().getStore().setActive(true);
        invitedEmployee.getEmployee().getStore().getCompany().setActive(true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        when(invitationRepository.hasPendingForUser(2)).thenReturn(true);
        when(invitationRepository.hasRecentForUser(eq(2), any(OffsetDateTime.class))).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.resendInvitation(2, administrator.getEmail()));
        verify(invitationRepository, never()).invalidateUnusedForUser(2);
        verify(invitationRepository, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Should invalidate old email tokens and send new invitation when email changes")
    void shouldInvalidateOldEmailTokensAndSendNewInvitationWhenEmailChanges() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser invitedEmployee = user(2, "old@example.com", "OPERATOR", false);

        invitedEmployee.getEmployee().setName("Employee");
        invitedEmployee.getEmployee().setCpf(ProtectedDataToken.cpf("12345678901"));
        invitedEmployee.getEmployee().setEmail("old@example.com");

        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("admin@example.com"))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(invitedEmployee));
        when(tokenHashService.newToken()).thenReturn("new-invite-token");
        when(tokenHashService.hash("new-invite-token")).thenReturn("new-token-hash");
        TransactionSynchronizationManager.initSynchronization();
        service.update(2, new UpdateEmployeeRequest("Employee", " New@Example.com ", null, null), "admin@example.com");

        assertEquals("new@example.com", invitedEmployee.getEmail());
        assertEquals("new@example.com", invitedEmployee.getEmployee().getEmail());

        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(2), any(OffsetDateTime.class));
        verify(invitationRepository).invalidateUnusedForUser(2);
        verify(sessionRepository).revokeAllForUser(2);
        verify(invitationRepository).create(eq(2), eq("new-token-hash"), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Should revoke sessions when employee role changes")
    void shouldRevokeSessionsWhenEmployeeRoleChanges() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser employee = user(2, "employee@example.com", "OPERATOR", true);
        employee.getEmployee().setCpf(ProtectedDataToken.cpf("12345678901"));
        employee.getEmployee().getRole().setId(3);

        EmployeeRole nextRole = new EmployeeRole();
        nextRole.setId(4);
        nextRole.setName("MANAGER");
        nextRole.setActive(true);

        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(employee));
        when(roleRepository.findByIdAndDeletedAtIsNull(4)).thenReturn(Optional.of(nextRole));

        service.update(2, new UpdateEmployeeRequest("Employee", employee.getEmail(), null, 4), administrator.getEmail());

        assertEquals(ProtectedDataToken.cpf("12345678901"), employee.getEmployee().getCpf());
        verify(employeeRepository, never()).existsByCpf(any());
        verify(sessionRepository).revokeAllForUser(2);
        verify(employeeRepository).saveAndFlush(employee.getEmployee());
        verify(auditLogRepository).record(eq("employee"), eq("UPDATE"), eq("2"), eq(1), any(), any());
    }

    @Test
    @DisplayName("Should preserve sessions when employee role and email remain unchanged")
    void shouldPreserveSessionsWhenRoleAndEmailRemainUnchanged() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);
        AppUser employee = user(2, "employee@example.com", "OPERATOR", true);
        employee.getEmployee().setCpf(ProtectedDataToken.cpf("12345678901"));
        employee.getEmployee().getRole().setId(3);
        employee.getEmployee().getRole().setActive(true);

        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(employee));
        when(roleRepository.findByIdAndDeletedAtIsNull(3)).thenReturn(Optional.of(employee.getEmployee().getRole()));

        service.update(2, new UpdateEmployeeRequest("Employee", employee.getEmail(), null, 3), administrator.getEmail());

        verify(sessionRepository, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("Should reject manager updates to employee records")
    void shouldRejectManagerEmployeeUpdate() {
        authenticateAs("MANAGER");
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser manager = user(1, "manager@example.com", "MANAGER", true);
        AppUser employee = user(2, "employee@example.com", "OPERATOR", true);
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(manager.getEmail()))
            .thenReturn(Optional.of(manager));
        when(appUserRepository.findByEmployeeId(2)).thenReturn(Optional.of(employee));
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(2, new UpdateEmployeeRequest("Employee", "employee@example.com", null, null), manager.getEmail()));

        verify(appUserRepository, never()).findByIdWithWriteLock(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create an inactive employee and send the invitation after commit")
    void shouldCreateInactiveEmployeeWithInvitation() {
        EmployeeService service = new EmployeeService(appUserRepository, employeeRepository, roleRepository, storeRepository, activityRepository, invitationRepository, passwordResetTokenRepository, sessionRepository, auditLogRepository, emailService, passwordEncoder, tokenHashService, emailFailureService, entityManager);
        AppUser administrator = user(1, "admin@example.com", "ADMINISTRATOR", true);

        EmployeeRole employeeRole = new EmployeeRole();
        employeeRole.setId(3);
        employeeRole.setName("OPERATOR");
        employeeRole.setActive(true);

        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(administrator.getEmail()))
            .thenReturn(Optional.of(administrator));
        when(storeRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1))
            .thenReturn(Optional.of(administrator.getEmployee().getStore()));
        when(roleRepository.findByIdAndDeletedAtIsNull(3)).thenReturn(Optional.of(employeeRole));
        when(tokenHashService.newToken()).thenReturn("temporary-password", "invitation-token");
        when(tokenHashService.hash("invitation-token")).thenReturn("invitation-hash");
        when(passwordEncoder.encode("temporary-password")).thenReturn("encoded-password");
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(2);
            return employee;
        });
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser savedUser = invocation.getArgument(0);
            savedUser.setId(2);
            return savedUser;
        });
        TransactionSynchronizationManager.initSynchronization();
        var response = service.create(new CreateEmployeeRequest("  Employee  ", "12345678901", " Employee@Example.com ", "  555-0100  ",
                3, 1, LocalDate.now()), administrator.getEmail());
        assertFalse(response.active());
        assertEquals("Employee", response.name());
        assertEquals("employee@example.com", response.email());
        assertEquals("555-0100", response.phone());
        assertEquals("***", response.cpf());

        String cpfToken = ProtectedDataToken.cpf("12345678901");
        verify(employeeRepository).existsByCpf(cpfToken);
        verify(employeeRepository).saveAndFlush(argThat(employee -> cpfToken.equals(employee.getCpf())));
        verify(appUserRepository).saveAndFlush(argThat(user -> cpfToken.equals(user.getCpf())
                && cpfToken.equals(user.getEmployee().getCpf())));
        verify(invitationRepository).create(eq(2), eq("invitation-hash"), any(OffsetDateTime.class));
        verify(emailService, never()).sendInvitationLink(any(), any());
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(emailService).sendInvitationLink("employee@example.com", "invitation-token");
    }

    private AppUser user(Integer id, String email, String roleName, Boolean passwordSet) {
        Company company = new Company();
        company.setId(1);
        RetailStore store = new RetailStore();
        store.setId(1);
        store.setCompany(company);
        EmployeeRole role = new EmployeeRole();
        role.setName(roleName);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setStore(store);
        employee.setRole(role);
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail(email);
        user.setEmployee(employee);
        user.setPasswordSet(passwordSet);
        return user;
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

}
