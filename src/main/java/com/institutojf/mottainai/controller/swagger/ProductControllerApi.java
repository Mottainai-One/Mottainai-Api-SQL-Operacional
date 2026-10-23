package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateProductRequest;
import com.institutojf.mottainai.dto.request.UpdateProductRequest;
import com.institutojf.mottainai.dto.request.UpdateActiveStatusRequest;
import com.institutojf.mottainai.dto.response.ProductResponse;
import com.institutojf.mottainai.dto.request.BatchStorePriceRequest;
import com.institutojf.mottainai.dto.request.LinkSupplierToProductRequest;
import com.institutojf.mottainai.dto.request.UpdateStorePriceRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierProductRequest;
import com.institutojf.mottainai.dto.response.ProductHistoryResponse;
import com.institutojf.mottainai.dto.response.ProductPriceHistoryResponse;
import com.institutojf.mottainai.dto.response.StoreProductPriceResponse;
import com.institutojf.mottainai.dto.response.SupplierProductResponse;
import com.institutojf.mottainai.dto.response.SupplierResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Products", description = "API for managing products")
public interface ProductControllerApi {

    @Operation(summary = "Create a product", description = "The company is resolved from the authenticated user account; do not include companyId in the request body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Product category not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Barcode already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductResponse> create(CreateProductRequest request, Authentication authentication);

    @Operation(summary = "Find all products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product list found", content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    })
    ResponseEntity<Page<ProductResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductResponse> findById(Integer id);

    @Operation(summary = "Find a product by barcode")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductResponse> findByBarcode(String barcode);

    @Operation(summary = "Update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductResponse> update(Integer id, UpdateProductRequest request);

    @Operation(summary = "Change product active status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<ProductResponse> updateStatus(Integer id, UpdateActiveStatusRequest request);

    @Operation(summary = "Logically delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product logically deleted"),
            @ApiResponse(responseCode = "400", description = "Product cannot be logically deleted", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id, Authentication authentication);

    @Operation(summary = "List active store prices for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<StoreProductPriceResponse>> findStorePrices(Integer id);

    @Operation(summary = "Set a product price at a store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<StoreProductPriceResponse> updateStorePrice(Integer id, Integer storeId, UpdateStorePriceRequest request, Authentication authentication);

    @Operation(summary = "Update multiple store prices atomically")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<StoreProductPriceResponse>> updatePrices(BatchStorePriceRequest request, Authentication authentication);

    @Operation(summary = "List suppliers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<Page<SupplierResponse>> findSuppliers(@ParameterObject Pageable pageable);

    @Operation(summary = "List suppliers linked to a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<Page<SupplierProductResponse>> findSuppliersByProduct(Integer id, @ParameterObject Pageable pageable);

    @Operation(summary = "Link a supplier to a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<SupplierProductResponse> linkSupplier(Integer id, LinkSupplierToProductRequest request, Authentication authentication);

    @Operation(summary = "Update supplier terms for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<SupplierProductResponse> updateSupplierLink(Integer id, Integer supplierId, UpdateSupplierProductRequest request, Authentication authentication);

    @Operation(summary = "Deactivate a supplier-product link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<Void> deactivateSupplierLink(Integer id, Integer supplierId, Authentication authentication);

    @Operation(summary = "List product master-data history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<ProductHistoryResponse>> findMasterHistory(Integer id, LocalDateTime from, LocalDateTime to);

    @Operation(summary = "List product price history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<ProductPriceHistoryResponse>> findPriceHistory(Integer id, LocalDateTime from, LocalDateTime to);

}
