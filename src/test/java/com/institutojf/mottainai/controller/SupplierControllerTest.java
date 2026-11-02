package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateSupplierRequest;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.response.AddressResponse;
import com.institutojf.mottainai.dto.response.SupplierResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.SupplierService;
import com.institutojf.mottainai.service.SupplierProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SupplierService supplierService;

    @Mock
    private SupplierProductService supplierProductService;

    @InjectMocks
    private SupplierController supplierController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supplierController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create supplier with location header")
    void shouldCreateSupplierWithLocationHeader() throws Exception {
        when(supplierService.create(any())).thenReturn(response());
        mockMvc
            .perform(post("/api/v1/suppliers").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/suppliers/1"))
            .andExpect(jsonPath("$.tradeName").value("Fornecedor Teste"));
        verify(supplierService).create(any());
    }

    @Test
    @DisplayName("Should reject invalid supplier request")
    void shouldRejectInvalidSupplierRequest() throws Exception {
        mockMvc
            .perform(post("/api/v1/suppliers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":1,\"tradeName\":\"Fornecedor Teste\",\"cnpj\":\"invalid\"}"))
            .andExpect(status().isBadRequest());
        verify(supplierService, never()).create(any());
    }

    @Test
    @DisplayName("Should deactivate supplier with no content response")
    void shouldDeactivateSupplierWithNoContentResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/1")).andExpect(status().isNoContent());
        verify(supplierService).delete(1);
    }

    private CreateSupplierRequest request() {
        return new CreateSupplierRequest(new CreateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP"), "Fornecedor Teste", "11222333000181", "fornecedor@teste.com", "11999999999");
    }

    private SupplierResponse response() {
        AddressResponse address = new AddressResponse(1, "05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
        return new SupplierResponse(1, address, "Fornecedor Teste", "11222333000181", "fornecedor@teste.com", "11999999999", true);
    }

}
