package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.response.BatchResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchService batchService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BatchController batchController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(batchController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should change only the batch active status")
    void shouldChangeOnlyTheBatchActiveStatus() throws Exception {
        when(batchService.updateStatus(eq(1), eq(false), any(Authentication.class))).thenReturn(response());
        mockMvc
            .perform(patch("/api/v1/batches/1/status").principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.active").value(false));
        verify(batchService).updateStatus(eq(1), eq(false), any(Authentication.class));
    }

    private BatchResponse response() {
        return new BatchResponse(1, 1, "LOT-001", LocalDate.now(), LocalDate.now().plusDays(30), BigDecimal.ONE,
                BigDecimal.TEN, false, 1);
    }

}
