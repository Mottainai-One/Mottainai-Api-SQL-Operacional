package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.EmployeeRoleControllerApi;
import com.institutojf.mottainai.dto.response.EmployeeRoleResponse;
import com.institutojf.mottainai.service.EmployeeRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-roles")
@RequiredArgsConstructor
public class EmployeeRoleController implements EmployeeRoleControllerApi {
    private final EmployeeRoleService employeeRoleService;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<List<EmployeeRoleResponse>> findAll() {
        return ResponseEntity.ok(employeeRoleService.findAll());
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<EmployeeRoleResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(employeeRoleService.findById(id));
    }
}
