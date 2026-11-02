package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.response.AddressResponse;
import com.institutojf.mottainai.dto.response.CompanyResponse;
import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.RetailStoreService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RetailStoreControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RetailStoreService retailStoreService;

    @InjectMocks
    private RetailStoreController retailStoreController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(retailStoreController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create store with location header")
    void shouldCreateStoreWithLocationHeader() throws Exception {
        when(retailStoreService.create(any())).thenReturn(response());
        mockMvc
            .perform(post("/api/v1/stores").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/stores/1"))
            .andExpect(jsonPath("$.name").value("Loja Centro"));
        verify(retailStoreService).create(any());
    }

    @Test
    @DisplayName("Should reject store without company")
    void shouldRejectStoreWithoutCompany() throws Exception {
        mockMvc
            .perform(post("/api/v1/stores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":1,\"name\":\"Loja Centro\",\"cnpj\":\"11222333000181\"}"))
            .andExpect(status().isBadRequest());
        verify(retailStoreService, never()).create(any());
    }

    @Test
    @DisplayName("Should reject store with longitude outside valid range")
    void shouldRejectStoreWithInvalidLongitude() throws Exception {
        mockMvc.perform(post("/api/v1/stores").contentType(MediaType.APPLICATION_JSON)
            .content(
                    "{\"companyId\":1,\"addressId\":1,\"name\":\"Loja Centro\",\"cnpj\":\"11222333000181\",\"longitude\":181}"))
            .andExpect(status().isBadRequest());
        verify(retailStoreService, never()).create(any());
    }

    @Test
    @DisplayName("Should deactivate store with no content response")
    void shouldDeactivateStoreWithNoContentResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/stores/1")
            .principal(new UsernamePasswordAuthenticationToken("admin@mottainai.com", null)))
            .andExpect(status().isNoContent());
        verify(retailStoreService).delete(eq(1), any());
    }

    @Test
    @DisplayName("Should change only the store active status")
    void shouldChangeOnlyTheStoreActiveStatus() throws Exception {
        when(retailStoreService.updateStatus(1, false)).thenReturn(response());
        mockMvc
            .perform(patch("/api/v1/stores/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
        verify(retailStoreService).updateStatus(eq(1), eq(false));
    }

    private CreateRetailStoreRequest request() {
        return new CreateRetailStoreRequest(1, new CreateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP"), "Loja Centro", "11222333000181", "loja@mottainai.com", "11999999999", null, null);
    }

    private RetailStoreResponse response() {
        SubscriptionPlanResponse plan = new SubscriptionPlanResponse(1, "Basic", "Entry plan", new BigDecimal("99.90"), 2, 5, true);
        CompanyResponse company = new CompanyResponse(1, plan, "Mottainai Comercio LTDA", "Mottainai", "11222333000181", "contato@mottainai.com", "11999999999", null, null, true);
        AddressResponse address = new AddressResponse(1, "05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
        return new RetailStoreResponse(1, company, address, "Loja Centro", "11222333000181", "loja@mottainai.com", "11999999999", null, null, true);
    }

}
