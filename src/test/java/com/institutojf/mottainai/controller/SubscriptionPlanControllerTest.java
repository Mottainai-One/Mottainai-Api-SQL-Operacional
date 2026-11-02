package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.SubscriptionPlanService;
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

import java.math.BigDecimal;

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
class SubscriptionPlanControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @InjectMocks
    private SubscriptionPlanController subscriptionPlanController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionPlanController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create subscription plan with location header")
    void shouldCreateSubscriptionPlanWithLocationHeader() throws Exception {
        when(subscriptionPlanService.create(any())).thenReturn(response());
        mockMvc
            .perform(post("/api/v1/subscription-plans").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/subscription-plans/1"))
            .andExpect(jsonPath("$.name").value("Basic"));
        verify(subscriptionPlanService).create(any());
    }

    @Test
    @DisplayName("Should reject subscription plan with non positive store limit")
    void shouldRejectSubscriptionPlanWithNonPositiveStoreLimit() throws Exception {
        mockMvc
            .perform(post("/api/v1/subscription-plans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Basic\",\"price\":99.90,\"storeLimit\":0,\"userLimit\":5}"))
            .andExpect(status().isBadRequest());
        verify(subscriptionPlanService, never()).create(any());
    }

    @Test
    @DisplayName("Should deactivate subscription plan with no content response")
    void shouldDeactivateSubscriptionPlanWithNoContentResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/subscription-plans/1")).andExpect(status().isNoContent());
        verify(subscriptionPlanService).delete(1);
    }

    private CreateSubscriptionPlanRequest request() {
        return new CreateSubscriptionPlanRequest("Basic", "Entry plan", new BigDecimal("99.90"), 2, 5);
    }

    private SubscriptionPlanResponse response() {
        return new SubscriptionPlanResponse(1, "Basic", "Entry plan", new BigDecimal("99.90"), 2, 5, true);
    }

}
