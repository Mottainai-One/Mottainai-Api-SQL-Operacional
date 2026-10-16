package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.InviteStoreUserRequest;
import com.institutojf.mottainai.dto.request.UpdateStoreUserRequest;
import com.institutojf.mottainai.dto.response.InviteStoreUserResponse;
import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.dto.response.UserResponse;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.RetailStoreMapper;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.EmployeeRoleRepository;
import com.institutojf.mottainai.security.ProtectedDataToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository appUserRepository;

    private final EmployeeRepository employeeRepository;

    private final EmployeeRoleRepository employeeRoleRepository;

    private final AuditLogRepository auditLogRepository;

    private final RetailStoreMapper retailStoreMapper;

    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        return toResponse(findUser(email));
    }

    @Transactional(readOnly = true)
    public RetailStoreResponse myStore(String email) {
        return retailStoreMapper.toResponse(findUser(email).getEmployee().getStore());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return appUserRepository.findAllByDeletedAtIsNull().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        return toResponse(appUserRepository.findById(id)
            .filter(user -> user.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Transactional
    public InviteStoreUserResponse invite(InviteStoreUserRequest request, String requesterEmail) {
        AppUser requester = findUser(requesterEmail);
        String email = StaffContactData.email(request.email());
        String cpfToken = ProtectedDataToken.cpf(request.cpf());
        if (appUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new ConflictException("Email already exists");
        }
        if (employeeRepository.existsByCpf(cpfToken)) {
            throw new ConflictException("CPF already exists");
        }
        Employee employee = new Employee();
        employee.setStore(requester.getEmployee().getStore());
        employee.setRole(findRole(request.role()));
        employee.setName(StaffContactData.name(request.name()));
        employee.setCpf(cpfToken);
        employee.setEmail(email);
        employee.setPhone(StaffContactData.phone(request.phone()));
        employee.setActive(false);
        employee = employeeRepository.save(employee);
        AppUser user = new AppUser();
        user.setEmployee(employee);
        user.setEmail(email);
        user.setCpf(cpfToken);
        user.setPasswordHash(passwordEncoder.encode(generateRandomSecret()));
        user.setActive(false);
        user = appUserRepository.save(user);
        auditLogRepository.record("employee", "INSERT", employee.getId().toString(), requester.getId(), null,
                Map.of("name", employee.getName(), "cpf_masked", maskCpf(employee.getCpf()), "email", employee.getEmail(), "role_id",
                        employee.getRole().getId(), "store_id", employee.getStore().getId(), "active", false));
        auditLogRepository.record("app_user", "INSERT", user.getId().toString(), requester.getId(), null, Map
            .of("employee_id", employee.getId(), "email", user.getEmail(), "cpf_masked", maskCpf(employee.getCpf()), "active", false));
        return new InviteStoreUserResponse(toResponse(user), false, true, false);
    }

    @Transactional
    public UserResponse update(Integer id, UpdateStoreUserRequest request, String requesterEmail) {
        AppUser requester = findUser(requesterEmail);
        AppUser user = findByIdEntity(id);
        if (request.role() == null && request.active() == null) {
            throw new IllegalArgumentException("At least one field must be provided");
        }
        Employee employee = user.getEmployee();
        Map<String, Object> oldEmployeeData = Map.of("role_id", employee.getRole().getId(), "active",
                employee.getActive());
        boolean oldUserActive = user.getActive();
        if (request.role() != null) {
            employee.setRole(findRole(request.role()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
            employee.setActive(request.active());
        }
        user = appUserRepository.save(user);
        auditLogRepository.record("employee", "UPDATE", employee.getId().toString(), requester.getId(), oldEmployeeData,
                Map.of("role_id", employee.getRole().getId(), "active", employee.getActive()));
        auditLogRepository.record("app_user", "UPDATE", user.getId().toString(), requester.getId(),
                Map.of("active", oldUserActive), Map.of("active", user.getActive()));
        return toResponse(user);
    }

    private AppUser findUser(String email) {
        return appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(StaffContactData.email(email))
            .filter(user -> Boolean.TRUE.equals(user.getActive())
                    && Boolean.TRUE.equals(user.getEmployee().getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AppUser findByIdEntity(Integer id) {
        return appUserRepository.findById(id)
            .filter(user -> user.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private EmployeeRole findRole(String role) {
        return employeeRoleRepository.findByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull(role)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private UserResponse toResponse(AppUser user) {
        Employee employee = user.getEmployee();
        return new UserResponse(user.getId(), employee.getName(), maskCpf(employee.getCpf()), user.getEmail(),
                employee.getPhone(), employee.getRole().getName(), user.getActive(), employee.getStore().getId());
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "***";
        }
        return "***.***.***-" + cpf.substring(9);
    }

    private String generateRandomSecret() {
        return Long.toUnsignedString(secureRandom.nextLong()) + Long.toUnsignedString(secureRandom.nextLong());
    }

}
