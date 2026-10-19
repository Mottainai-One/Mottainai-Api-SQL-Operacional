package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateEmployeeRequest;
import com.institutojf.mottainai.dto.request.EmployeeStatusRequest;
import com.institutojf.mottainai.dto.request.UpdateEmployeeRequest;
import com.institutojf.mottainai.dto.response.EmployeeCancelRequestResponse;
import com.institutojf.mottainai.dto.response.EmployeeResponse;
import com.institutojf.mottainai.dto.response.EmployeeShiftResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.InvitationDeliveryException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
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
import com.institutojf.mottainai.security.TokenHashService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final AppUserRepository appUserRepository;

    private final EmployeeRepository employeeRepository;

    private final EmployeeRoleRepository roleRepository;

    private final RetailStoreRepository storeRepository;

    private final EmployeeActivityRepository activityRepository;

    private final EmployeeInvitationTokenRepository invitationRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final StaffSessionRepository sessionRepository;

    private final AuditLogRepository auditLogRepository;

    private final PasswordResetEmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final TokenHashService tokenHashService;

    private final EmployeePrivateDataService privateDataService;

    private final StaffEmailFailureService emailFailureService;

    private final EntityManager entityManager;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request, String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        String email = StaffContactData.email(request.email());
        String cpf = StaffContactData.cpf(request.cpf());
        RetailStore store = storeRepository.findByIdAndActiveTrueAndDeletedAtIsNull(request.storeId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        if (!store.getCompany().getId().equals(requestingUser.getEmployee().getStore().getCompany().getId())) {
            throw new ResourceNotFoundException("Store not found");
        }
        if (privateDataService.cpfExists(cpf, null)) {
            throw new ConflictException("CPF already exists");
        }
        if (privateDataService.emailExists(email, null)) {
            throw new ConflictException("Email already exists");
        }
        Employee employee = new Employee();
        employee.setStore(store);
        employee.setRole(role(request.roleId()));
        employee.setHireDate(request.hireDate());
        employee.setActive(false);
        employee = employeeRepository.saveAndFlush(employee);
        privateDataService.save(employee, request.name(), cpf, email, request.phone());
        AppUser user = new AppUser();
        user.setEmployee(employee);
        user.setPasswordHash(passwordEncoder.encode(tokenHashService.newToken()));
        user.setPasswordSet(false);
        user.setActive(false);
        user = appUserRepository.saveAndFlush(user);
        String rawToken = tokenHashService.newToken();
        invitationRepository.create(user.getId(), tokenHashService.hash(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(48));
        auditLogRepository.record("employee", "INSERT", employee.getId().toString(), requestingUser.getId(), null,
                Map.of("store_id", store.getId(), "role_id", employee.getRole().getId()));
        auditLogRepository.record("app_user", "INSERT", user.getId().toString(), requestingUser.getId(), null,
                Map.of("employee_id", employee.getId(), "active", false));
        scheduleInvitationEmail(email, rawToken, employee.getId());
        return response(user);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listStore(String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        return appUserRepository.findEmployeesByStore(requestingUser.getEmployee().getStore().getId())
            .stream()
            .map(this::response)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listCompany(String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        Integer companyId = requestingUser.getEmployee().getStore().getCompany().getId();
        return appUserRepository.findEmployeesByCompany(companyId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse find(Integer employeeId, String requestingUserEmail) {
        return response(target(employeeId, requestingUser(requestingUserEmail), false));
    }

    @Transactional
    public EmployeeResponse update(Integer employeeId, UpdateEmployeeRequest request, String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        AppUser user = target(employeeId, requestingUser, true);
        Employee employee = user.getEmployee();
        String cpf = StaffContactData.cpf(privateDataService.cpf(employee));
        String email = StaffContactData.email(request.email());
        boolean emailChanged = !java.util.Objects.equals(privateDataService.email(employee), email);
        boolean roleChanged = request.roleId() != null && !request.roleId().equals(employee.getRole().getId());

        if (emailChanged && privateDataService.emailExists(email, employee.getId())) {
            throw new ConflictException("Email already exists");
        }
        Map<String, Object> oldData = employeeAuditData(employee);
        privateDataService.save(employee, request.name(), cpf, email, request.phone());
        if (request.roleId() != null) {
            employee.setRole(role(request.roleId()));
        }
        employeeRepository.saveAndFlush(employee);
        appUserRepository.save(user);
        auditLogRepository.record("employee", "UPDATE", employeeId.toString(), requestingUser.getId(), oldData,
                employeeAuditData(employee));
        if (emailChanged || roleChanged) {
            sessionRepository.revokeAllForUser(user.getId());
        }
        if (emailChanged) {
            passwordResetTokenRepository.invalidateUnusedForUser(user.getId(), OffsetDateTime.now(ZoneOffset.UTC));
            invitationRepository.invalidateUnusedForUser(user.getId());
            if (!Boolean.TRUE.equals(user.getPasswordSet())) {
                String rawToken = tokenHashService.newToken();
                invitationRepository.create(user.getId(), tokenHashService.hash(rawToken),
                        OffsetDateTime.now(ZoneOffset.UTC).plusHours(48));
                scheduleInvitationEmail(email, rawToken, employeeId);
            }
            auditLogRepository.record("app_user", "UPDATE", user.getId().toString(), requestingUser.getId(),
                    Map.of("email_changed", true), Map.of("email_changed", true));
        }
        return response(user);
    }

    @Transactional
    public void delete(Integer employeeId, String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        AppUser user = target(employeeId, requestingUser, true);
        if (user.getId().equals(requestingUser.getId())) {
            throw new BusinessException("You cannot delete your own access");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        boolean oldActive = Boolean.TRUE.equals(user.getActive());
        user.getEmployee().setDeletedAt(now);
        user.getEmployee().setActive(false);
        user.setDeletedAt(now);
        user.setActive(false);
        employeeRepository.save(user.getEmployee());
        appUserRepository.save(user);
        sessionRepository.revokeAllForUser(user.getId());
        invitationRepository.invalidateUnusedForUser(user.getId());
        auditLogRepository.record("employee", "DELETE", employeeId.toString(), requestingUser.getId(),
                Map.of("active", oldActive), Map.of("active", false, "deleted_at", now.toString()));
    }

    @Transactional
    public EmployeeResponse changeStatus(Integer employeeId, EmployeeStatusRequest request, String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        AppUser user = target(employeeId, requestingUser, true);
        if (user.getId().equals(requestingUser.getId()) && !request.active()) {
            throw new BusinessException("You cannot disable your own access");
        }
        if (request.active() && !Boolean.TRUE.equals(user.getPasswordSet())) {
            throw new BusinessException("The employee must define a password before activation");
        }
        if (request.active() && (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank())) {
            throw new BusinessException("The employee must link a Firebase account before activation");
        }
        if (request.active() && (!Boolean.TRUE.equals(user.getEmployee().getRole().getActive())
                || user.getEmployee().getRole().getDeletedAt() != null
                || !Boolean.TRUE.equals(user.getEmployee().getStore().getActive())
                || user.getEmployee().getStore().getDeletedAt() != null)) {
            throw new BusinessException("The employee's role and store must be active before activation");
        }
        boolean oldActive = Boolean.TRUE.equals(user.getActive());
        user.setActive(request.active());
        user.getEmployee().setActive(request.active());
        employeeRepository.save(user.getEmployee());
        appUserRepository.save(user);
        if (!request.active()) {
            passwordResetTokenRepository.invalidateUnusedForUser(user.getId(), OffsetDateTime.now(ZoneOffset.UTC));
            sessionRepository.revokeAllForUser(user.getId());
            invitationRepository.invalidateUnusedForUser(user.getId());
        }
        auditLogRepository.record("employee", "UPDATE", employeeId.toString(), requestingUser.getId(),
                Map.of("active", oldActive), Map.of("active", request.active()));
        return response(user);
    }

    @Transactional
    public EmployeeResponse resendInvitation(Integer employeeId, String requestingUserEmail) {
        AppUser requestingUser = requestingUser(requestingUserEmail);
        AppUser user = target(employeeId, requestingUser, true);
        if (Boolean.TRUE.equals(user.getPasswordSet()) || Boolean.TRUE.equals(user.getActive())
                || Boolean.TRUE.equals(user.getEmployee().getActive())) {
            throw new BusinessException("Only an inactive employee without a password can be invited again");
        }
        Employee employee = user.getEmployee();
        if (!Boolean.TRUE.equals(employee.getRole().getActive()) || employee.getRole().getDeletedAt() != null
                || !Boolean.TRUE.equals(employee.getStore().getActive()) || employee.getStore().getDeletedAt() != null
                || !Boolean.TRUE.equals(employee.getStore().getCompany().getActive())
                || employee.getStore().getCompany().getDeletedAt() != null) {
            throw new BusinessException("The employee's role, store and company must be active before inviting again");
        }
        if (invitationRepository.hasPendingForUser(user.getId()) && invitationRepository.hasRecentForUser(user.getId(),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5))) {
            throw new BusinessException("Wait five minutes before sending another invitation");
        }
        passwordResetTokenRepository.invalidateUnusedForUser(user.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        invitationRepository.invalidateUnusedForUser(user.getId());
        String rawToken = tokenHashService.newToken();
        invitationRepository.create(user.getId(), tokenHashService.hash(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(48));
        auditLogRepository.record("app_user", "UPDATE", user.getId().toString(), requestingUser.getId(), null,
                Map.of("invitation_issued", true));
        scheduleInvitationEmail(privateDataService.email(employee), rawToken, employeeId);
        return response(user);
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> shifts(Integer employeeId, LocalDateTime from, LocalDateTime to, String requestingUserEmail) {
        requireActivityAccess(employeeId, requestingUserEmail);
        validateRange(from, to);
        return activityRepository.findShifts(employeeId, from, to);
    }

    @Transactional(readOnly = true)
    public List<EmployeeCancelRequestResponse> cancelRequests(Integer employeeId, LocalDateTime from, LocalDateTime to, String requestingUserEmail) {
        requireActivityAccess(employeeId, requestingUserEmail);
        validateRange(from, to);
        return activityRepository.findCancelRequests(employeeId, from, to);
    }

    private AppUser requestingUser(String firebaseUid) {
        return appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void requireActivityAccess(Integer employeeId, String requestingUserEmail) {
        AppUser requester = requestingUser(requestingUserEmail);
        if (!hasRole("ADMINISTRATOR") && !hasRole("MANAGER")) {
            throw new ResourceNotFoundException("Employee not found");
        }
        target(employeeId, requester, false);
    }

    private AppUser target(Integer employeeId, AppUser requestingUser, boolean write) {
        AppUser found = appUserRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (write) {
            entityManager.refresh(found, LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(found.getEmployee(), LockModeType.PESSIMISTIC_WRITE);
            if (found.getDeletedAt() != null || found.getEmployee().getDeletedAt() != null) {
                throw new ResourceNotFoundException("Employee not found");
            }
        }
        Integer requestingUserCompany = requestingUser.getEmployee().getStore().getCompany().getId();
        Integer targetCompany = found.getEmployee().getStore().getCompany().getId();
        boolean administrator = hasRole("ADMINISTRATOR");
        boolean manager = hasRole("MANAGER");
        boolean sameStore = requestingUser.getEmployee()
            .getStore()
            .getId()
            .equals(found.getEmployee().getStore().getId());
        boolean self = requestingUser.getId().equals(found.getId());
        if (!requestingUserCompany.equals(targetCompany) || (write && !administrator)
                || (!administrator && !(manager && sameStore) && !self)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        return found;
    }

    private boolean hasRole(String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private EmployeeRole role(Integer roleId) {
        return roleRepository.findByIdAndDeletedAtIsNull(roleId)
            .filter(found -> Boolean.TRUE.equals(found.getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A date range of at most six months is required");
        }
    }

    private EmployeeResponse response(AppUser user) {
        Employee employee = user.getEmployee();
        return new EmployeeResponse(employee.getId(), user.getId(),
                privateDataService.name(employee), privateDataService.cpfMasked(employee),
                privateDataService.email(employee), privateDataService.phone(employee),
                employee.getRole().getName(), employee.getStore().getId(),
                user.getActive(), user.getLastLogin());
    }

    private Map<String, Object> employeeAuditData(Employee employee) {
        Map<String, Object> data = new HashMap<>();
        data.put("role_id", employee.getRole().getId());
        return data;
    }

    private void scheduleInvitationEmail(String email, String rawToken, Integer employeeId) {
        String tokenHash = tokenHashService.hash(rawToken);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendInvitationLink(email, rawToken);
                }
                catch (MailException exception) {
                    log.error("Invitation email delivery failed for employee {}", employeeId, exception);
                    try {
                        emailFailureService.invalidateInvitationToken(tokenHash);
                    }
                    catch (RuntimeException cleanupException) {
                        log.error("Could not invalidate undelivered invitation for employee {}", employeeId,
                                cleanupException);
                    }
                    throw new InvitationDeliveryException(employeeId, exception);
                }
            }
        });
    }

}
