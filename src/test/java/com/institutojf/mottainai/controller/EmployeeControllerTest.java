package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.response.EmployeeResponse;
import com.institutojf.mottainai.exception.InvitationDeliveryException;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create an employee through the documented route")
    void shouldCreateEmployee() throws Exception {
        when(employeeService.create(any(), eq("admin@example.com"))).thenReturn(employee());

        mockMvc.perform(
                    post("/api/v1/employees").principal(principal()).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Employee","cpf":"12345678901","email":"employee@example.com",
                             "roleId":3,"storeId":1,"hireDate":"2026-09-01"}
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeId").value(2))
            .andExpect(jsonPath("$.active").value(false));
        verify(employeeService).create(any(), eq("admin@example.com"));
    }

    @Test
    @DisplayName("Should list employees from the authenticated store")
    void shouldListStoreEmployees() throws Exception {
        when(employeeService.listStore("admin@example.com")).thenReturn(List.of(employee()));

        mockMvc.perform(get("/api/v1/employees-store").principal(principal()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].employeeId").value(2));
        verify(employeeService).listStore("admin@example.com");
    }

    @Test
    @DisplayName("Should list employees from the authenticated company")
    void shouldListCompanyEmployees() throws Exception {
        when(employeeService.listCompany("admin@example.com")).thenReturn(List.of(employee()));

        mockMvc.perform(get("/api/v1/employees-company").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(2));

        verify(employeeService).listCompany("admin@example.com");
    }

    @Test
    @DisplayName("Should find an employee by id")
    void shouldFindEmployee() throws Exception {
        when(employeeService.find(2, "admin@example.com")).thenReturn(employee());

        mockMvc.perform(get("/api/v1/employees/2").principal(principal()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employeeId").value(2));
        verify(employeeService).find(2, "admin@example.com");
    }

    @Test
    @DisplayName("Should update employee data by id")
    void shouldUpdateEmployee() throws Exception {
        when(employeeService.update(eq(2), any(), eq("admin@example.com"))).thenReturn(employee());
        mockMvc.perform(
                put("/api/v1/employees/2").principal(principal()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Employee","email":"employee@example.com"}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("employee@example.com"));
        verify(employeeService).update(eq(2), any(), eq("admin@example.com"));
    }

    @Test
    @DisplayName("Should logically delete an employee by id")
    void shouldDeleteEmployee() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/2").principal(principal())).andExpect(status().isNoContent());
        verify(employeeService).delete(2, "admin@example.com");
    }

    @Test
    @DisplayName("Should change employee status by id")
    void shouldChangeEmployeeStatus() throws Exception {
        when(employeeService.changeStatus(eq(2), any(), eq("admin@example.com"))).thenReturn(employee());
        mockMvc
            .perform(put("/api/v1/employees/2/status").principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
        verify(employeeService).changeStatus(eq(2), any(), eq("admin@example.com"));
    }

    @Test
    @DisplayName("Should report committed employee when invitation delivery fails")
    void shouldReportInvitationDeliveryFailure() throws Exception {
        when(employeeService.resendInvitation(2, "admin@example.com"))
            .thenThrow(new InvitationDeliveryException(2, new RuntimeException("SMTP unavailable")));
        mockMvc.perform(post("/api/v1/employees/2/invite").principal(principal()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message")
                .value(containsString("POST /api/v1/employees/2/invite")));
    }

    @Test
    @DisplayName("Should resend an invitation through the administrator route")
    void shouldResendInvitation() throws Exception {
        when(employeeService.resendInvitation(2, "admin@example.com")).thenReturn(employee());
        mockMvc.perform(post("/api/v1/employees/2/invite").principal(principal()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employeeId").value(2))
            .andExpect(jsonPath("$.active").value(false));
        verify(employeeService).resendInvitation(2, "admin@example.com");
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken("admin@example.com", null);
    }

    private EmployeeResponse employee() {
        return new EmployeeResponse(2, 2, "Employee", "***456789**", "employee@example.com", null, "OPERATOR", 1, false, null);
    }

}
