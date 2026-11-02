package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateProductCategoryRequest;
import com.institutojf.mottainai.dto.response.ProductCategoryResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.ProductCategoryService;
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
class ProductCategoryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductCategoryService categoryService;

    @InjectMocks
    private ProductCategoryController productCategoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productCategoryController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create category with location header")
    void shouldCreateCategoryWithLocationHeader() throws Exception {
        when(categoryService.create(any())).thenReturn(new ProductCategoryResponse(1, "Food", "Food products", true));
        mockMvc
            .perform(post("/api/v1/product-categories").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductCategoryRequest("Food", "Food products"))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/product-categories/1"))
            .andExpect(jsonPath("$.name").value("Food"));
        verify(categoryService).create(any());
    }

    @Test
    @DisplayName("Should reject invalid category request")
    void shouldRejectInvalidCategoryRequest() throws Exception {
        mockMvc
            .perform(post("/api/v1/product-categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest());
        verify(categoryService, never()).create(any());
    }

    @Test
    @DisplayName("Should deactivate category with no content response")
    void shouldDeactivateCategoryWithNoContentResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/product-categories/1")).andExpect(status().isNoContent());
        verify(categoryService).delete(1);
    }

}
