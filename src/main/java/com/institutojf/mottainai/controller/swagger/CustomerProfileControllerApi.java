package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "CustomerProfile", description = "Authenticated customer profile")
public interface CustomerProfileControllerApi {

    @Operation(summary = "Get the current customer profile using a Firebase ID token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer profile found", content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "401", description = "Firebase authentication required"),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CustomerResponse clientProfile(Authentication authentication);

    @Operation(summary = "Create the SQL customer profile for the authenticated Firebase identity")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer profile created", content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid profile data or Firebase token claims", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Firebase authentication required"),
            @ApiResponse(responseCode = "409", description = "CPF or email conflict, or identity linked to an inactive customer", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CustomerResponse createClientProfile(@Valid @RequestBody CreateFirebaseCustomerProfileRequest request,
            Jwt token);

    @Operation(summary = "Update editable fields of the current Firebase customer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer profile updated", content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid profile fields", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Firebase authentication required"),
            @ApiResponse(responseCode = "404", description = "Customer profile not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CustomerResponse updateClientProfile(@Valid @RequestBody UpdateFirebaseCustomerProfileRequest request,
            Jwt token);

}
