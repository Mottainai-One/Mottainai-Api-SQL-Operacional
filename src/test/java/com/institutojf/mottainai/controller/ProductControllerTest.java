package com.institutojf.mottainai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.dto.request.CreateProductRequest;
import com.institutojf.mottainai.dto.response.ProductResponse;
import com.institutojf.mottainai.handler.GlobalExceptionHandler;
import com.institutojf.mottainai.service.ProductService;
import com.institutojf.mottainai.service.ProductCommercialService;
import com.institutojf.mottainai.service.SupplierProductService;
import com.institutojf.mottainai.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @Mock
    private ProductCommercialService productCommercialService;

    @Mock
    private SupplierProductService supplierProductService;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Should create product with location header")
    void shouldCreateProductWithLocationHeader() throws Exception {
        when(productService.create(any(), any())).thenReturn(response());
        mockMvc
            .perform(post("/api/v1/products").principal(new UsernamePasswordAuthenticationToken("admin", null)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/products/1"))
            .andExpect(jsonPath("$.barcode").value("7891234567890"));
        verify(productService).create(any(), any());
    }

    @Test
    @DisplayName("Should reject invalid product request")
    void shouldRejectInvalidProductRequest() throws Exception {
        mockMvc
            .perform(post("/api/v1/products").principal(new UsernamePasswordAuthenticationToken("admin", null)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"categoryId\":1,\"barcode\":\"\",\"name\":\"Rice\",\"unitMeasure\":\"KG\"}"))
            .andExpect(status().isBadRequest());
        verify(productService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Should reject product weight with more than three decimal places")
    void shouldRejectProductWeightWithMoreThanThreeDecimalPlaces() throws Exception {
        mockMvc.perform(post("/api/v1/products").principal(new UsernamePasswordAuthenticationToken("admin", null)).contentType(MediaType.APPLICATION_JSON)
            .content(
                    "{\"categoryId\":1,\"barcode\":\"7891234567890\",\"name\":\"Rice\",\"unitMeasure\":\"KG\",\"weight\":1.1234}"))
            .andExpect(status().isBadRequest());
        verify(productService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Should reject product with invalid CEST format")
    void shouldRejectProductWithInvalidCestFormat() throws Exception {
        mockMvc.perform(post("/api/v1/products").principal(new UsernamePasswordAuthenticationToken("admin", null)).contentType(MediaType.APPLICATION_JSON)
            .content(
                    "{\"categoryId\":1,\"taxProfileId\":1,\"barcode\":\"7891234567890\",\"ncm\":\"12345678\",\"cest\":\"123456\",\"name\":\"Rice\",\"unitMeasure\":\"KG\"}"))
            .andExpect(status().isBadRequest());
        verify(productService, never()).create(any(), any());
    }

    @Test
    @DisplayName("Should find product by barcode")
    void shouldFindProductByBarcode() throws Exception {
        when(productService.findByBarcode("7891234567890")).thenReturn(response());
        mockMvc.perform(get("/api/v1/products/barcode/7891234567890"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Rice"));
        verify(productService).findByBarcode("7891234567890");
    }

    @Test
    @DisplayName("Should deactivate product with no content response")
    void shouldDeactivateProductWithNoContentResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1")
            .principal(new UsernamePasswordAuthenticationToken("admin@mottainai.com", null)))
            .andExpect(status().isNoContent());
        verify(productService).delete(eq(1), any());
    }

    @Test
    @DisplayName("Should change only the product active status")
    void shouldChangeOnlyTheProductActiveStatus() throws Exception {
        when(productService.updateStatus(1, false)).thenReturn(response());
        mockMvc
            .perform(patch("/api/v1/products/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
        verify(productService).updateStatus(eq(1), eq(false));
    }

    private CreateProductRequest request() {
        return new CreateProductRequest(1, 1, "7891234567890", "12345678", null, "Rice", null, null, "KG", BigDecimal.ONE);
    }

    private ProductResponse response() {
        return new ProductResponse(1, 1, 1, "Food", 1, "RIC-001", "7891234567890", "12345678", null, "Rice", null, null, "KG", BigDecimal.ONE, true, 1);
    }

}
