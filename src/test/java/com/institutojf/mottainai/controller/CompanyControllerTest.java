package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateCompanyRequest;
import com.institutojf.mottainai.dto.response.CompanyResponse;
import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.CompanyService;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private CompanyController companyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(companyController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create company with location header")
    void shouldCreateCompanyWithLocationHeader() throws Exception {
        when(companyService.create(any())).thenReturn(response());
        mockMvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/companies/1"))
            .andExpect(jsonPath("$.officialName").value("Mottainai Comercio LTDA"));
        verify(companyService).create(any());
    }

    @Test
    @DisplayName("Should reject company with invalid CNPJ format")
    void shouldRejectCompanyWithInvalidCnpjFormat() throws Exception {
        mockMvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON)
            .content(
                    "{\"planId\":1,\"officialName\":\"Mottainai\",\"cnpj\":\"invalid\",\"email\":\"contato@mottainai.com\"}"))
            .andExpect(status().isBadRequest());
        verify(companyService, never()).create(any());
    }

    @Test
    @DisplayName("Should reject company with latitude outside valid range")
    void shouldRejectCompanyWithInvalidLatitude() throws Exception {
        mockMvc.perform(post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON)
            .content(
                    "{\"planId\":1,\"officialName\":\"Mottainai\",\"cnpj\":\"11222333000181\",\"email\":\"contato@mottainai.com\",\"latitude\":91}"))
            .andExpect(status().isBadRequest());
        verify(companyService, never()).create(any());
    }

    @Test
    @DisplayName("Should deactivate company with no content response")
    void shouldDeactivateCompanyWithNoContentResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/companies/1")
            .principal(new UsernamePasswordAuthenticationToken("admin@mottainai.com", null)))
            .andExpect(status().isNoContent());
        verify(companyService).delete(eq(1), any());
    }

    private CreateCompanyRequest request() {
        return new CreateCompanyRequest(1, "Mottainai Comercio LTDA", "Mottainai", "11222333000181", "contato@mottainai.com", "11999999999", null, null);
    }

    private CompanyResponse response() {
        SubscriptionPlanResponse plan = new SubscriptionPlanResponse(1, "Basic", "Entry plan", new BigDecimal("99.90"), 2, 5, true);
        return new CompanyResponse(1, plan, "Mottainai Comercio LTDA", "Mottainai", "11222333000181", "contato@mottainai.com", "11999999999", null, null, true);
    }

}
