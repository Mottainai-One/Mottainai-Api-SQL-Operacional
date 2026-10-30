package com.institutojf.mottainai.security;

import com.institutojf.mottainai.controller.CustomerController;
import com.institutojf.mottainai.controller.CustomerProfileController;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.CustomerAuthRepository;
import com.institutojf.mottainai.service.CustomerAuthenticationService;
import com.institutojf.mottainai.service.CustomerService;
import com.institutojf.mottainai.service.LoyaltyService;
import com.institutojf.mottainai.service.RlsContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({ CustomerController.class, CustomerProfileController.class })
@Import({ SecurityConfig.class, DatabaseUserDetailsService.class })
@TestPropertySource(properties = { "security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "security.jwt.issuer=https://mottainai.local", "security.jwt.expiration-minutes=60" })
class CustomerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CustomerService customerService;

    @MockitoBean
    LoyaltyService loyaltyService;

    @MockitoBean
    CustomerAuthenticationService customerAuthenticationService;

    @MockitoBean
    CustomerAccess customerAccess;

    @MockitoBean
    AppUserRepository appUserRepository;

    @MockitoBean
    RlsContextService rlsContextService;

    @MockitoBean
    CustomerAuthRepository customerAuthRepository;

    @Test
    @DisplayName("Should block customer from administrative customer list")
    void blocksCustomerFromAdministrativeCustomerList() throws Exception {
        mockMvc.perform(get("/api/v1/customers").with(user("ana@example.com").roles("CUSTOMER")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(customerService);
    }

    @Test
    @DisplayName("Should permit customer to read own record")
    void permitsCustomerToReadOwnRecord() throws Exception {
        when(customerService.find(7)).thenReturn(new CustomerResponse(7, null, true, false, null, null));
        mockMvc.perform(get("/api/v1/customers/7").with(user("ana@example.com").roles("CUSTOMER")))
            .andExpect(status().isOk());
        verify(customerAccess).checkAccess(any(), eq(7));
    }

    @Test
    @DisplayName("Should permit administrator to list customers")
    void permitsAdministratorToListCustomers() throws Exception {
        when(customerService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/customers").with(user("admin@example.com").roles("ADMINISTRATOR")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should serialize only the documented operational customer metadata")
    void exposesOperationalCustomerResponseContract() throws Exception {
        when(customerService.find(7)).thenReturn(new CustomerResponse(7, (short) 1999, true, false, null, null));

        mockMvc.perform(get("/api/v1/customers/7").with(user("admin@example.com").roles("ADMINISTRATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.birthYear").value(1999))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.marketingConsent").value(false))
            .andExpect(jsonPath("$.fullName").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.cpf").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.email").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.phone").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.externalAuthUid").doesNotHaveJsonPath());
    }

    @Test
    @DisplayName("Should reject customer access to another record before invoking the service")
    void rejectsCustomerAccessToAnotherRecord() throws Exception {
        doThrow(new AccessDeniedException("Customer cannot access this record")).when(customerAccess).checkAccess(any(), eq(8));

        mockMvc.perform(get("/api/v1/customers/8").with(user("ana@example.com").roles("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(customerService);
    }

    @Test
    @DisplayName("Should require Firebase authentication for app customer profile")
    void requiresFirebaseAuthenticationForClientProfile() throws Exception {
        mockMvc.perform(get("/api/v1/client/auth/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/client/auth/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Ana\",\"cpf\":\"12345678909\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/client/auth/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Ana\",\"marketingConsent\":false}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(customerAuthenticationService);
    }

    @Test
    @DisplayName("Should allow Firebase customer to read its own profile")
    void permitsFirebaseCustomerProfile() throws Exception {
        mockMvc.perform(get("/api/v1/client/auth/profile").with(jwt().jwt(token -> token.subject("firebase-uid"))))
            .andExpect(status().isOk());
        verify(customerAuthenticationService).profile(any());
    }

    @Test
    @DisplayName("Should create the SQL profile for an authenticated Firebase customer")
    void createsFirebaseCustomerProfile() throws Exception {
        when(customerService.createFirebaseProfile(any(), eq("firebase-uid"), eq("ana@example.com")))
            .thenReturn(new CustomerResponse(7, null, true, false, null, null));

        mockMvc.perform(post("/api/v1/client/auth/profile")
                .with(jwt().jwt(token -> token.subject("firebase-uid").claim("email", "ana@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Ana\",\"cpf\":\"12345678909\"}"))
            .andExpect(status().isCreated());

        verify(customerService).createFirebaseProfile(any(), eq("firebase-uid"), eq("ana@example.com"));
    }

    @Test
    @DisplayName("Should update only the authenticated Firebase customer's editable profile fields")
    void updatesFirebaseCustomerProfile() throws Exception {
        when(customerService.updateFirebaseProfile(any(), eq("firebase-uid")))
            .thenReturn(new CustomerResponse(7, null, true, true, null, null));

        mockMvc.perform(put("/api/v1/client/auth/profile")
                .with(jwt().jwt(token -> token.subject("firebase-uid")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Ana Silva\",\"phone\":\"11999999999\",\"marketingConsent\":true}"))
            .andExpect(status().isOk());

        verify(customerService).updateFirebaseProfile(any(), eq("firebase-uid"));
    }

}
