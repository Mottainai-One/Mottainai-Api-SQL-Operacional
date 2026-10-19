package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.EmployeeRoleRequest;
import com.institutojf.mottainai.dto.response.EmployeeRoleResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.EmployeeRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeRoleService {

    private static final Set<String> AUTHORIZATION_ROLES = Set.of("ADMINISTRATOR", "MANAGER");

    private final EmployeeRoleRepository employeeRoleRepository;

    private final EmployeeRepository employeeRepository;

    private final AppUserRepository appUserRepository;

    private final AuditLogRepository auditLogRepository;

    private final StaffUserLookup staffUserLookup;

    @Transactional(readOnly = true)
    public List<EmployeeRoleResponse> findAll() {
        return employeeRoleRepository.findAllByActiveTrueAndDeletedAtIsNullOrderByPermissionLevelDescNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeRoleResponse findById(Integer id) {
        return toResponse(findActiveRole(id));
    }

    @Transactional
    public EmployeeRoleResponse create(EmployeeRoleRequest request, String actorEmail) {
        if (employeeRoleRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Role name already exists");
        }
        AppUser actor = findActor(actorEmail);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        EmployeeRole role = new EmployeeRole();
        apply(role, request, now);
        role.setActive(true);
        role.setCreatedAt(now);
        role = employeeRoleRepository.save(role);
        auditLogRepository.record("employee_role", "INSERT", role.getId().toString(), actor.getId(), null, toAuditData(role));
        return toResponse(role);
    }

    @Transactional
    public EmployeeRoleResponse update(Integer id, EmployeeRoleRequest request, String actorEmail) {
        EmployeeRole role = findActiveRole(id);
        String newName = request.name().trim();
        if (AUTHORIZATION_ROLES.contains(role.getName().toUpperCase(Locale.ROOT))
                && !role.getName().equalsIgnoreCase(newName)) {
            throw new BusinessException("Authorization role names cannot be changed");
        }
        if (employeeRoleRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new ConflictException("Role name already exists");
        }
        AppUser actor = findActor(actorEmail);
        EmployeeRoleAudit oldData = toAuditData(role);
        apply(role, request, LocalDateTime.now(ZoneOffset.UTC));
        role = employeeRoleRepository.save(role);
        auditLogRepository.record("employee_role", "UPDATE", role.getId().toString(), actor.getId(), oldData, toAuditData(role));
        return toResponse(role);
    }

    @Transactional
    public void delete(Integer id, String actorEmail) {
        EmployeeRole role = findActiveRole(id);
        if (employeeRepository.countByRole_IdAndDeletedAtIsNull(id) > 0) {
            throw new BusinessException("Reassign employees before deactivating this role");
        }
        AppUser actor = findActor(actorEmail);
        EmployeeRoleAudit oldData = toAuditData(role);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        role.setActive(false);
        role.setDeletedAt(now);
        role.setUpdatedAt(now);
        employeeRoleRepository.save(role);
        auditLogRepository.record("employee_role", "UPDATE", role.getId().toString(), actor.getId(), oldData, toAuditData(role));
    }

    private EmployeeRole findActiveRole(Integer id) {
        return employeeRoleRepository.findByIdAndDeletedAtIsNull(id)
            .filter(role -> Boolean.TRUE.equals(role.getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Employee role not found"));
    }

    private AppUser findActor(String email) {
        return staffUserLookup.findActive(email)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void apply(EmployeeRole role, EmployeeRoleRequest request, LocalDateTime updatedAt) {
        role.setName(request.name().trim());
        role.setDescription(request.description());
        role.setPermissionLevel(request.permissionLevel());
        role.setUpdatedAt(updatedAt);
    }

    private EmployeeRoleResponse toResponse(EmployeeRole role) {
        return new EmployeeRoleResponse(role.getId(), role.getName(), role.getDescription(), role.getPermissionLevel(), role.getActive());
    }

    private EmployeeRoleAudit toAuditData(EmployeeRole role) {
        return new EmployeeRoleAudit(role.getName(), role.getDescription(), role.getPermissionLevel(), role.getActive());
    }

    private record EmployeeRoleAudit(String name, String description, Integer permissionLevel, Boolean active) {
    }

}
