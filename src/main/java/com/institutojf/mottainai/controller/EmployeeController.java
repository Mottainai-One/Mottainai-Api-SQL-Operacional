package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.EmployeeControllerApi;
import com.institutojf.mottainai.dto.request.CreateEmployeeRequest;
import com.institutojf.mottainai.dto.request.EmployeeStatusRequest;
import com.institutojf.mottainai.dto.request.UpdateEmployeeRequest;
import com.institutojf.mottainai.dto.response.EmployeeCancelRequestResponse;
import com.institutojf.mottainai.dto.response.EmployeeResponse;
import com.institutojf.mottainai.dto.response.EmployeeShiftResponse;
import com.institutojf.mottainai.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmployeeController implements EmployeeControllerApi {
    private final EmployeeService employeeService;

    @Override
    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request, authentication.getName()));
    }

    @Override
    @GetMapping("/employees-store")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<List<EmployeeResponse>> listStore(Authentication authentication) {
        return ResponseEntity.ok(employeeService.listStore(authentication.getName()));
    }

    @Override
    @GetMapping("/employees-company")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<EmployeeResponse>> listCompany(Authentication authentication) {
        return ResponseEntity.ok(employeeService.listCompany(authentication.getName()));
    }

    @Override
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> find(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(employeeService.find(id, authentication.getName()));
    }

    @Override
    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateEmployeeRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeeService.update(id, request, authentication.getName()));
    }

    @Override
    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        employeeService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/employees/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<EmployeeResponse> changeStatus(@PathVariable Integer id, @Valid @RequestBody EmployeeStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeeService.changeStatus(id, request, authentication.getName()));
    }

    @Override
    @PostMapping("/employees/{id}/invite")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<EmployeeResponse> resendInvitation(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(employeeService.resendInvitation(id, authentication.getName()));
    }

    @Override
    @GetMapping("/employees/{id}/shifts")
    public ResponseEntity<List<EmployeeShiftResponse>> shifts(@PathVariable Integer id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return ResponseEntity.ok(employeeService.shifts(id, from, to, authentication.getName()));
    }

    @Override
    @GetMapping("/employees/{id}/cancel-request")
    public ResponseEntity<List<EmployeeCancelRequestResponse>> cancelRequests(@PathVariable Integer id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return ResponseEntity.ok(employeeService.cancelRequests(id, from, to, authentication.getName()));
    }

}
