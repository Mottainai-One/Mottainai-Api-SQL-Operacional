package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateSupplierRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierRequest;
import com.institutojf.mottainai.dto.response.SupplierResponse;
import com.institutojf.mottainai.dto.request.LinkProductToSupplierRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierProductRequest;
import com.institutojf.mottainai.dto.response.SupplierProductResponse;
import com.institutojf.mottainai.dto.response.SupplierPurchaseHistoryResponse;
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

@Tag(name = "Suppliers", description = "API for managing suppliers")
public interface SupplierControllerApi {

    @Operation(summary = "Create a supplier", description = "The address CEP automatically fills street, neighborhood, city and state through BrasilAPI. Number and complement are entered manually.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Supplier created", content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Address not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CNPJ already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SupplierResponse> create(CreateSupplierRequest request);

    @Operation(summary = "Find all suppliers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier list found", content = @Content(schema = @Schema(implementation = SupplierResponse.class)))
    })
    ResponseEntity<Page<SupplierResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find a supplier by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier found", content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SupplierResponse> findById(Integer id);

    @Operation(summary = "Update a supplier", description = "The address CEP is looked up through BrasilAPI on update. Number and complement remain manual fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier updated", content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SupplierResponse> update(Integer id, UpdateSupplierRequest request);

    @Operation(summary = "Logically delete a supplier")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Supplier logically deleted"),
            @ApiResponse(responseCode = "400", description = "Supplier cannot be logically deleted", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id);

    @Operation(summary = "List purchase history for a supplier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<SupplierPurchaseHistoryResponse>> findHistory(Integer id, LocalDateTime from, LocalDateTime to);

    @Operation(summary = "List products supplied by a supplier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<Page<SupplierProductResponse>> findProducts(Integer id, @ParameterObject Pageable pageable);

    @Operation(summary = "Link a product to a supplier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<SupplierProductResponse> linkProduct(Integer id, LinkProductToSupplierRequest request, Authentication authentication);

    @Operation(summary = "Update commercial terms for a supplied product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<SupplierProductResponse> updateProductLink(Integer id, Integer productId, UpdateSupplierProductRequest request, Authentication authentication);

    @Operation(summary = "Deactivate a supplier-product link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<Void> deactivateProductLink(Integer id, Integer productId, Authentication authentication);

}
