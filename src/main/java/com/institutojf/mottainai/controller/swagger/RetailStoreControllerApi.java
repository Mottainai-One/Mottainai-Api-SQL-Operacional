package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.UpdateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.UpdateActiveStatusRequest;
import com.institutojf.mottainai.dto.response.RetailStoreResponse;
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

@Tag(name = "Retail Stores", description = "API for managing retail stores")
public interface RetailStoreControllerApi {

    @Operation(summary = "Create a retail store")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Retail store created", content = @Content(schema = @Schema(implementation = RetailStoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Company or address not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CNPJ already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<RetailStoreResponse> create(CreateRetailStoreRequest request);

    @Operation(summary = "Find all retail stores")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retail store list found", content = @Content(schema = @Schema(implementation = RetailStoreResponse.class)))
    })
    ResponseEntity<Page<RetailStoreResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find a retail store by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retail store found", content = @Content(schema = @Schema(implementation = RetailStoreResponse.class))),
            @ApiResponse(responseCode = "404", description = "Retail store not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<RetailStoreResponse> findById(Integer id);

    @Operation(summary = "Update a retail store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retail store updated", content = @Content(schema = @Schema(implementation = RetailStoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Retail store not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<RetailStoreResponse> update(Integer id, UpdateRetailStoreRequest request);

    @Operation(summary = "Change retail store active status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<RetailStoreResponse> updateStatus(Integer id, UpdateActiveStatusRequest request);

    @Operation(summary = "Logically delete a retail store")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Retail store logically deleted"),
            @ApiResponse(responseCode = "400", description = "Retail store cannot be logically deleted", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Retail store not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id, Authentication authentication);

}
