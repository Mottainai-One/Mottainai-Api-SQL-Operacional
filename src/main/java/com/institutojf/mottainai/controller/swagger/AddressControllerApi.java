package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.dto.response.AddressResponse;
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

@Tag(name = "Addresses", description = "API for managing addresses")
public interface AddressControllerApi {

    @Operation(summary = "Create an address", description = "Send a CEP and number. Street, neighborhood, city and state are filled from BrasilAPI; supplied values are used when the provider leaves a field blank. Complement is kept as entered.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created", content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<AddressResponse> create(CreateAddressRequest request);

    @Operation(summary = "Find all addresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address list found", content = @Content(schema = @Schema(implementation = AddressResponse.class)))
    })
    ResponseEntity<Page<AddressResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find an address by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address found", content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "404", description = "Address not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<AddressResponse> findById(Integer id);

    @Operation(summary = "Update an address", description = "The CEP is looked up again and address fields are refreshed. Number and complement are kept as entered; missing provider fields can be supplied manually.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated", content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Address not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<AddressResponse> update(Integer id, UpdateAddressRequest request);
}
