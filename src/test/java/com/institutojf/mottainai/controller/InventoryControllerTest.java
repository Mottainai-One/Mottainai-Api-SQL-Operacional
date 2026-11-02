package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.response.InventoryResponse;
import com.institutojf.mottainai.model.enums.InventoryType;
import com.institutojf.mottainai.service.InventoryMovementService;
import com.institutojf.mottainai.service.InventoryService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryMovementService inventoryMovementService;

    @InjectMocks
    private InventoryController inventoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController).build();
    }

    @Test
    @DisplayName("Should return created for new inventory")
    void shouldReturnCreatedForNewInventory() throws Exception {
        when(inventoryService.create(any(), any())).thenReturn(inventory(false));
        mockMvc
            .perform(post("/api/v1/inventory").principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reactivated").value(false));
    }

    @Test
    @DisplayName("Should return ok when existing inventory is reactivated")
    void shouldReturnOkWhenExistingInventoryIsReactivated() throws Exception {
        when(inventoryService.create(any(), any())).thenReturn(inventory(true));
        mockMvc
            .perform(post("/api/v1/inventory").principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reactivated").value(true));
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken("admin@example.com", null);
    }

    private String request() {
        return "{\"batchId\":3,\"minimumQuantity\":2.000}";
    }

    private InventoryResponse inventory(boolean reactivated) {
        return new InventoryResponse(4, 2, 3, InventoryType.NORMAL, BigDecimal.TEN, new BigDecimal("2.000"), null, null, 1, reactivated);
    }

}
