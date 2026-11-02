package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.EmployeeRoleResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.EmployeeRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeRoleService {

    private final EmployeeRoleRepository employeeRoleRepository;

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

    private EmployeeRole findActiveRole(Integer id) {
        return employeeRoleRepository.findByIdAndDeletedAtIsNull(id)
            .filter(role -> Boolean.TRUE.equals(role.getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Employee role not found"));
    }

    private EmployeeRoleResponse toResponse(EmployeeRole role) {
        return new EmployeeRoleResponse(role.getId(), role.getName(), role.getDescription(), role.getPermissionLevel(), role.getActive());
    }
}
