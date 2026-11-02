package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.response.AddressResponse;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.AddressService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should return created address with location header")
    void shouldReturnCreatedAddressWithLocationHeader() throws Exception {
        when(addressService.create(any())).thenReturn(response());
        mockMvc
            .perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/addresses/1"))
            .andExpect(jsonPath("$.street").value("Rua Irineu José Bordon"));
        verify(addressService).create(any());
    }

    @Test
    @DisplayName("Should return bad request when address request is invalid")
    void shouldReturnBadRequestWhenAddressRequestIsInvalid() throws Exception {
        CreateAddressRequest request = new CreateAddressRequest("invalid", "Rua Irineu José Bordon", "335", null,
                "Vila Jaguara", "São Paulo", "SP");
        mockMvc
            .perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
        verify(addressService, never()).create(any());
    }

    @Test
    @DisplayName("Should return not found when address does not exist")
    void shouldReturnNotFoundWhenAddressDoesNotExist() throws Exception {
        when(addressService.findById(1)).thenThrow(new ResourceNotFoundException("Address not found"));
        mockMvc.perform(get("/api/v1/addresses/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Address not found"));
    }

    @Test
    @DisplayName("Should return conflict when address already exists")
    void shouldReturnConflictWhenAddressAlreadyExists() throws Exception {
        when(addressService.create(any())).thenThrow(new ConflictException("Address already exists"));
        mockMvc
            .perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Address already exists"));
    }

    private CreateAddressRequest request() {
        return new CreateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
    }

    private AddressResponse response() {
        return new AddressResponse(1, "05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
    }

}
