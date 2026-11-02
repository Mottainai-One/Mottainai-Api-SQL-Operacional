package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateCompanyRequest;
import com.institutojf.mottainai.dto.request.UpdateCompanyRequest;
import com.institutojf.mottainai.dto.response.CompanyResponse;
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

@Tag(name = "Companies", description = "API for managing companies")
public interface CompanyControllerApi {

    @Operation(summary = "Create a company")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Company created", content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CNPJ already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<CompanyResponse> create(CreateCompanyRequest request);

    @Operation(summary = "Find all companies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company list found", content = @Content(schema = @Schema(implementation = CompanyResponse.class)))
    })
    ResponseEntity<Page<CompanyResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find a company by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company found", content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<CompanyResponse> findById(Integer id);

    @Operation(summary = "Update a company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company updated", content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<CompanyResponse> update(Integer id, UpdateCompanyRequest request);

    @Operation(summary = "Logically delete a company")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Company logically deleted"),
            @ApiResponse(responseCode = "400", description = "Company cannot be logically deleted", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Company not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id, Authentication authentication);

}
