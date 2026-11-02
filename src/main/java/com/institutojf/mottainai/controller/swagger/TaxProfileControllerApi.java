package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.TaxProfileRequest;
import com.institutojf.mottainai.dto.response.TaxProfileResponse;
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

@Tag(name = "Tax Profiles", description = "Manage tax profiles used by products")
public interface TaxProfileControllerApi {

    @Operation(summary = "List active tax profiles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax profiles found", content = @Content(schema = @Schema(implementation = TaxProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    ResponseEntity<Page<TaxProfileResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Get a tax profile with its tax rules")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax profile found", content = @Content(schema = @Schema(implementation = TaxProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tax profile not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<TaxProfileResponse> findById(Integer id);

    @Operation(summary = "Create a tax profile")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tax profile created", content = @Content(schema = @Schema(implementation = TaxProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid tax rates or request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Tax profile code already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<TaxProfileResponse> create(TaxProfileRequest request, Authentication authentication);

    @Operation(summary = "Update tax rates and rules")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax profile updated", content = @Content(schema = @Schema(implementation = TaxProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tax profile not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<TaxProfileResponse> update(Integer id, TaxProfileRequest request, Authentication authentication);

    @Operation(summary = "Logically delete a tax profile")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tax profile logically deleted"),
            @ApiResponse(responseCode = "400", description = "Tax profile is used by active products", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Tax profile not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id, Authentication authentication);

}
