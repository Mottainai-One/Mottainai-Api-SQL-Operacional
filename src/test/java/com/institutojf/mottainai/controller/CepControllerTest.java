package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.response.CepResponse;
import com.institutojf.mottainai.exception.CepNotFoundException;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.BrasilApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CepController tests")
class CepControllerTest {

    @Mock
    private BrasilApiService brasilApiService;

    @InjectMocks
    private CepController cepController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cepController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should return 200 with address data when CEP exists")
    void shouldReturn200WithAddressDataWhenCepExists() throws Exception {
        CepResponse mockResponse = new CepResponse("01001000", "Praça da Sé", "Sé", "São Paulo", "SP", "open-cep");
        when(brasilApiService.getCep("01001000")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/cep/01001000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zipCode").value("01001000"))
            .andExpect(jsonPath("$.street").value("Praça da Sé"))
            .andExpect(jsonPath("$.neighborhood").value("Sé"))
            .andExpect(jsonPath("$.city").value("São Paulo"))
            .andExpect(jsonPath("$.state").value("SP"))
            .andExpect(jsonPath("$.service").value("open-cep"));
    }

    @Test
    @DisplayName("Should return 404 when CEP does not exist")
    void shouldReturn404WhenCepDoesNotExist() throws Exception {
        when(brasilApiService.getCep("99999999")).thenThrow(new CepNotFoundException("99999999"));
        mockMvc.perform(get("/api/v1/cep/99999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("CEP not found: 99999999"));
    }

    @Test
    @DisplayName("Should return CEP data with all fields populated")
    void shouldReturnCepDataWithAllFieldsPopulated() throws Exception {
        CepResponse mockResponse = new CepResponse("20040020", "Avenida Presidente Vargas", "Centro", "Rio de Janeiro", "RJ", "open-cep");
        when(brasilApiService.getCep("20040020")).thenReturn(mockResponse);
        mockMvc.perform(get("/api/v1/cep/20040020"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zipCode").isNotEmpty())
            .andExpect(jsonPath("$.street").isNotEmpty())
            .andExpect(jsonPath("$.neighborhood").isNotEmpty())
            .andExpect(jsonPath("$.city").isNotEmpty())
            .andExpect(jsonPath("$.state").isNotEmpty())
            .andExpect(jsonPath("$.service").isNotEmpty());
    }

}
