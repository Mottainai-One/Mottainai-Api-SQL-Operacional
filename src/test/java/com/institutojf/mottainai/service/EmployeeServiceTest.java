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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeRoleRepository roleRepository;
    @Mock private RetailStoreRepository storeRepository;
    @Mock private EmployeeActivityRepository activityRepository;
    @Mock private EmployeeInvitationTokenRepository invitationRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private StaffSessionRepository sessionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PasswordResetEmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.institutojf.mottainai.security.TokenHashService tokenHashService;
    @Mock private EmployeePrivateDataService privateDataService;
    @Mock private StaffEmailFailureService emailFailureService;
    @Mock private EntityManager entityManager;

    @InjectMocks private EmployeeService service;

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "11111111111"})
    void createRejectsCpfWithInvalidChecksumOrRepeatedDigitsBeforePersistence(String cpf) {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));

        var request = new CreateEmployeeRequest("New Employee", cpf, "new@example.com", null,
                2, 8, LocalDate.of(2026, 1, 1));

        assertThrows(BusinessException.class, () -> service.create(request, "admin-uid"));

        verifyNoInteractions(privateDataService);
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
        verify(appUserRepository, never()).saveAndFlush(any(AppUser.class));
    }

    @Test
    void updateRejectsExistingCpfWithInvalidChecksumBeforeSaving() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser employee = staffUser(20, 10, "employee-uid", "Operator", true, true, 5);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(employee));
        when(privateDataService.cpf(employee.getEmployee())).thenReturn("12345678901");
        assumeRole("ADMINISTRATOR");

        assertThrows(BusinessException.class, () -> service.update(10,
                new UpdateEmployeeRequest("Employee", "employee@example.com", null, null), "admin-uid"));

        verify(privateDataService, never()).save(any(Employee.class), any(), any(), any(), any());
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @BeforeEach
    void beginSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPendingInvitationWithoutFirebaseUid() {
        Company company = new Company();
        company.setId(3);
        company.setActive(true);
        RetailStore requestersStore = new RetailStore();
        requestersStore.setId(5);
        requestersStore.setCompany(company);
        Employee requesterEmployee = new Employee();
        requesterEmployee.setStore(requestersStore);
        AppUser requester = new AppUser();
        requester.setId(1);
        requester.setActive(true);
        requester.setEmployee(requesterEmployee);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(requester));

        RetailStore targetStore = new RetailStore();
        targetStore.setId(8);
        targetStore.setCompany(company);
        when(storeRepository.findByIdAndActiveTrueAndDeletedAtIsNull(8)).thenReturn(Optional.of(targetStore));
        EmployeeRole role = new EmployeeRole();
        role.setId(2);
        role.setActive(true);
        role.setName("OPERATOR");
        when(roleRepository.findByIdAndDeletedAtIsNull(2)).thenReturn(Optional.of(role));
        when(privateDataService.cpfExists("12345678909", null)).thenReturn(false);
        when(privateDataService.emailExists("new@example.com", null)).thenReturn(false);
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(10);
            return employee;
        });
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(20);
            return user;
        });
        when(tokenHashService.newToken()).thenReturn("placeholder-password", "invitation-secret");
        when(tokenHashService.hash("invitation-secret")).thenReturn("invitation-hash");
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-placeholder");
        when(privateDataService.name(any(Employee.class))).thenReturn("New Employee");
        when(privateDataService.cpfMasked(any(Employee.class))).thenReturn("***.***.***-**");
        when(privateDataService.email(any(Employee.class))).thenReturn("new@example.com");
        when(privateDataService.phone(any(Employee.class))).thenReturn(null);

        var response = service.create(new CreateEmployeeRequest("New Employee", "12345678909", "new@example.com", null,
                2, 8, LocalDate.of(2026, 1, 1)), "admin-uid");

        verify(privateDataService).save(any(Employee.class), org.mockito.ArgumentMatchers.eq("New Employee"),
                org.mockito.ArgumentMatchers.eq("12345678909"), org.mockito.ArgumentMatchers.eq("new@example.com"),
                org.mockito.ArgumentMatchers.isNull());
        verify(invitationRepository).create(org.mockito.ArgumentMatchers.eq(20),
                org.mockito.ArgumentMatchers.eq("invitation-hash"), any());
        verify(appUserRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(user ->
                !Boolean.TRUE.equals(user.getActive()) && Boolean.FALSE.equals(user.getPasswordSet())
                        && user.getFirebaseUid() == null));
        assertFalse(response.active());
    }

    @Test
    void refusesToReactivateAnAccountWithoutLinkedFirebaseUid() {
        Company company = new Company();
        company.setId(3);
        RetailStore store = new RetailStore();
        store.setId(5);
        store.setCompany(company);
        EmployeeRole role = new EmployeeRole();
        role.setId(2);
        role.setName("OPERATOR");
        role.setActive(true);
        Employee requesterEmployee = new Employee();
        requesterEmployee.setStore(store);
        requesterEmployee.setRole(role);
        requesterEmployee.setActive(true);
        AppUser requester = new AppUser();
        requester.setId(1);
        requester.setActive(true);
        requester.setEmployee(requesterEmployee);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(requester));

        Employee targetEmployee = new Employee();
        targetEmployee.setId(10);
        targetEmployee.setStore(store);
        targetEmployee.setRole(role);
        targetEmployee.setActive(false);
        AppUser target = new AppUser();
        target.setId(20);
        target.setEmployee(targetEmployee);
        target.setActive(false);
        target.setPasswordSet(true);
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(target));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-uid", null, "ROLE_ADMINISTRATOR"));

        org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> service.changeStatus(10, new EmployeeStatusRequest(true), "admin-uid"));
    }

    @Test
    void disablingEmployeeInvalidatesTokensAndRevokesAllSessions() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser employee = staffUser(20, 10, "employee-uid", "Operator", true, true, 8);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(employee));
        assumeRole("ADMINISTRATOR");
        stubEmployeeResponse(employee.getEmployee(), "Employee", "employee@example.com");

        var response = service.changeStatus(10, new EmployeeStatusRequest(false), "admin-uid");

        assertFalse(response.active());
        assertFalse(employee.getActive());
        assertFalse(employee.getEmployee().getActive());
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(20), any(OffsetDateTime.class));
        verify(invitationRepository).invalidateUnusedForUser(20);
        verify(sessionRepository).revokeAllForUser(20);
        verify(entityManager).refresh(employee.getEmployee(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void resendInvitationSendsOnlyAfterCommitAndUsesPendingEmployeeEmail() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser invited = staffUser(20, 10, null, "Operator", false, false, 8);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(invited));
        when(privateDataService.email(invited.getEmployee())).thenReturn("invite@example.com");
        when(tokenHashService.newToken()).thenReturn("new-invite-token");
        when(tokenHashService.hash("new-invite-token")).thenReturn("new-invite-hash");
        assumeRole("ADMINISTRATOR");

        service.resendInvitation(10, "admin-uid");

        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(20), any(OffsetDateTime.class));
        verify(invitationRepository).invalidateUnusedForUser(20);
        verify(invitationRepository).create(eq(20), eq("new-invite-hash"), any(OffsetDateTime.class));
        verify(emailService, never()).sendInvitationLink(any(), any());
        org.springframework.transaction.support.TransactionSynchronizationUtils.triggerAfterCommit();
        verify(emailService).sendInvitationLink("invite@example.com", "new-invite-token");
    }

    @Test
    void resendInvitationIsRateLimitedWhenARecentInvitationIsPending() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser invited = staffUser(20, 10, null, "Operator", false, false, 8);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(invited));
        when(invitationRepository.hasPendingForUser(20)).thenReturn(true);
        when(invitationRepository.hasRecentForUser(eq(20), any(OffsetDateTime.class))).thenReturn(true);
        assumeRole("ADMINISTRATOR");

        assertThrows(BusinessException.class, () -> service.resendInvitation(10, "admin-uid"));

        verify(invitationRepository, never()).invalidateUnusedForUser(20);
        verify(invitationRepository, never()).create(any(), any(), any());
    }

    @Test
    void resendInvitationInvalidatesUndeliveredTokenWhenEmailDeliveryFails() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser invited = staffUser(20, 10, null, "Operator", false, false, 8);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(invited));
        when(privateDataService.email(invited.getEmployee())).thenReturn("invite@example.com");
        when(tokenHashService.newToken()).thenReturn("new-invite-token");
        when(tokenHashService.hash("new-invite-token")).thenReturn("new-invite-hash");
        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService).sendInvitationLink("invite@example.com", "new-invite-token");
        assumeRole("ADMINISTRATOR");

        service.resendInvitation(10, "admin-uid");

        assertThrows(InvitationDeliveryException.class,
                org.springframework.transaction.support.TransactionSynchronizationUtils::triggerAfterCommit);
        verify(emailFailureService).invalidateInvitationToken("new-invite-hash");
    }

    @Test
    void changingPendingEmployeesEmailRotatesInvitationAndRevokesOldSessions() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser invited = staffUser(20, 10, null, "Operator", false, false, 8);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(invited));
        when(privateDataService.email(invited.getEmployee())).thenReturn("old@example.com", "new@example.com");
        when(privateDataService.cpf(invited.getEmployee())).thenReturn("12345678909");
        when(tokenHashService.newToken()).thenReturn("replacement-invite");
        when(tokenHashService.hash("replacement-invite")).thenReturn("replacement-hash");
        assumeRole("ADMINISTRATOR");

        service.update(10, new UpdateEmployeeRequest("Employee", " New@Example.com ", null, null), "admin-uid");

        verify(privateDataService).save(invited.getEmployee(), "Employee", "12345678909", "new@example.com", null);
        verify(passwordResetTokenRepository).invalidateUnusedForUser(eq(20), any(OffsetDateTime.class));
        verify(invitationRepository).invalidateUnusedForUser(20);
        verify(invitationRepository).create(eq(20), eq("replacement-hash"), any(OffsetDateTime.class));
        verify(sessionRepository).revokeAllForUser(20);
        org.springframework.transaction.support.TransactionSynchronizationUtils.triggerAfterCommit();
        verify(emailService).sendInvitationLink("new@example.com", "replacement-invite");
    }

    @Test
    void changingEmployeeRoleRevokesSessionsButUnchangedRoleDoesNot() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser employee = staffUser(20, 10, "employee-uid", "Operator", true, true, 8);
        employee.getEmployee().getRole().setId(2);
        EmployeeRole nextRole = new EmployeeRole();
        nextRole.setId(4);
        nextRole.setName("Manager");
        nextRole.setActive(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(employee));
        when(roleRepository.findByIdAndDeletedAtIsNull(4)).thenReturn(Optional.of(nextRole));
        when(privateDataService.email(employee.getEmployee())).thenReturn("employee@example.com");
        when(privateDataService.cpf(employee.getEmployee())).thenReturn("12345678909");
        assumeRole("ADMINISTRATOR");

        service.update(10, new UpdateEmployeeRequest("Employee", "employee@example.com", null, 4), "admin-uid");

        verify(sessionRepository).revokeAllForUser(20);
        verify(employeeRepository).saveAndFlush(employee.getEmployee());
    }

    @Test
    void preservesSessionsWhenRoleAndEmailAreUnchanged() {
        AppUser administrator = staffUser(1, 101, "admin-uid", "Administrator", true, true, 5);
        AppUser employee = staffUser(20, 10, "employee-uid", "Operator", true, true, 8);
        employee.getEmployee().getRole().setId(2);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("admin-uid"))
                .thenReturn(Optional.of(administrator));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(employee));
        when(roleRepository.findByIdAndDeletedAtIsNull(2)).thenReturn(Optional.of(employee.getEmployee().getRole()));
        when(privateDataService.email(employee.getEmployee())).thenReturn("employee@example.com");
        when(privateDataService.cpf(employee.getEmployee())).thenReturn("12345678909");
        assumeRole("ADMINISTRATOR");

        service.update(10, new UpdateEmployeeRequest("Employee", "employee@example.com", null, 2), "admin-uid");

        verify(sessionRepository, never()).revokeAllForUser(20);
    }

    @Test
    void managerCannotUpdateAnotherEmployeesRecord() {
        AppUser manager = staffUser(1, 101, "manager-uid", "Manager", true, true, 5);
        AppUser employee = staffUser(20, 10, "employee-uid", "Operator", true, true, 8);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("manager-uid"))
                .thenReturn(Optional.of(manager));
        when(appUserRepository.findByEmployeeId(10)).thenReturn(Optional.of(employee));
        assumeRole("MANAGER");

        assertThrows(ResourceNotFoundException.class, () -> service.update(10,
                new UpdateEmployeeRequest("Employee", "employee@example.com", null, null), "manager-uid"));

        verify(employeeRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
    }

    private void assumeRole(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("authenticated-staff-uid", null, "ROLE_" + role));
    }

    private AppUser staffUser(Integer userId, Integer employeeId, String firebaseUid, String roleName,
            boolean active, boolean passwordSet, Integer storeId) {
        Company company = new Company();
        company.setId(3);
        company.setActive(true);
        RetailStore store = new RetailStore();
        store.setId(storeId);
        store.setCompany(company);
        store.setActive(true);
        EmployeeRole role = new EmployeeRole();
        role.setId(roleName.equalsIgnoreCase("Operator") ? 2 : 3);
        role.setName(roleName);
        role.setActive(true);
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setStore(store);
        employee.setRole(role);
        employee.setActive(active);
        AppUser user = new AppUser();
        user.setId(userId);
        user.setFirebaseUid(firebaseUid);
        user.setEmployee(employee);
        user.setActive(active);
        user.setPasswordSet(passwordSet);
        user.setPasswordHash("bcrypt-hash");
        return user;
    }

    private void stubEmployeeResponse(Employee employee, String name, String email) {
        when(privateDataService.name(employee)).thenReturn(name);
        when(privateDataService.cpfMasked(employee)).thenReturn("***.***.***-**");
        when(privateDataService.email(employee)).thenReturn(email);
        when(privateDataService.phone(employee)).thenReturn(null);
    }
}
