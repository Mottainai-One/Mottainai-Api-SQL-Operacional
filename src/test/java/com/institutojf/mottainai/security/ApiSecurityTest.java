package com.institutojf.mottainai.security;

import com.institutojf.mottainai.controller.EmployeeController;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.service.EmployeeService;
import com.institutojf.mottainai.service.RlsContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ ApiSecurityTest.SecurityTestController.class, ApiSecurityTest.CatalogSecurityTestController.class,
        EmployeeController.class })
@Import({ SecurityConfig.class, DatabaseUserDetailsService.class, ApiSecurityTest.SecurityTestController.class,
        ApiSecurityTest.CatalogSecurityTestController.class })
@TestPropertySource(properties = { "security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "security.jwt.issuer=https://mottainai.local", "security.jwt.expiration-minutes=60",
        "security.cors.allowed-origins=https://app.example" })
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeController employeeController;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private RlsContextService rlsContextService;

    @MockitoBean
    private CustomerAuthRepository customerAuthRepository;

    @Test
    @DisplayName("Should reject unauthenticated API request")
    void shouldRejectUnauthenticatedApiRequest() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should forbid write operation for operator")
    void shouldForbidWriteOperationForOperator() throws Exception {
        mockMvc.perform(post("/api/v1/security-probe").with(user("operator@mottainai.com").roles("OPERATOR"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow write operation for manager")
    void shouldAllowWriteOperationForManager() throws Exception {
        mockMvc.perform(post("/api/v1/security-probe").with(user("manager@mottainai.com").roles("MANAGER"))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should allow idempotency header in browser preflight")
    void shouldAllowIdempotencyHeaderInPreflight() throws Exception {
        mockMvc
            .perform(options("/api/v1/loyalty/redeem").header("Origin", "https://app.example")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Idempotency-Key,Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Headers", containsString("Idempotency-Key")));
    }

    @Test
    @DisplayName("Should forbid PATCH operation for operator")
    void shouldForbidPatchForOperator() throws Exception {
        mockMvc.perform(patch("/api/v1/security-probe").with(user("operator@mottainai.com").roles("OPERATOR"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow PATCH operation for manager")
    void shouldAllowPatchForManager() throws Exception {
        mockMvc.perform(patch("/api/v1/security-probe").with(user("manager@mottainai.com").roles("MANAGER"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject customer access to a staff endpoint")
    void shouldRejectCustomerFromStaffEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe").with(user("customer@mottainai.com").roles("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require Firebase JWT authentication for customer catalog reads")
    void shouldRequireFirebaseJwtForCustomerCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/customer-catalog/security-probe"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/customer-catalog/security-probe")
                .with(jwt().jwt(token -> token.subject("firebase-uid"))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should proxy the secured employee controller with CGLIB")
    void shouldProxyEmployeeControllerWithCglib() {
        assertTrue(AopUtils.isCglibProxy(employeeController));
    }

    @Test
    @DisplayName("Should enforce administrator authorization on the employee controller")
    void shouldRejectManagerFromAdministratorMethod() throws Exception {
        mockMvc.perform(get("/api/v1/employees-company").with(user("manager@mottainai.com").roles("MANAGER"))).andExpect(status().isForbidden());
        verifyNoInteractions(employeeService);
    }

    @Test
    @DisplayName("Should allow administrator access through the employee controller proxy")
    void shouldAllowAdministratorThroughEmployeeControllerProxy() throws Exception {
        when(employeeService.listCompany("admin@mottainai.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/employees-company").with(user("admin@mottainai.com").roles("ADMINISTRATOR"))).andExpect(status().isOk());
        verify(employeeService).listCompany("admin@mottainai.com");
    }

    @RestController
    @RequestMapping("/api/v1/security-probe")
    static class SecurityTestController {

        @GetMapping
        public String read() {
            return "ok";
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public String create() {
            return "created";
        }

        @PatchMapping
        public String update() {
            return "updated";
        }
    }

    @RestController
    @RequestMapping("/api/v1/customer-catalog/security-probe")
    static class CatalogSecurityTestController {
        @GetMapping
        public String read() {
            return "ok";
        }
    }

}
