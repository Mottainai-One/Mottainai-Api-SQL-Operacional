package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.dto.request.CreateCustomerRequest;
import com.institutojf.mottainai.dto.request.UpdateCustomerRequest;
import com.institutojf.mottainai.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Customer", description = "API for Customer")
public interface CustomerControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<CustomerResponse> findAll();

    @Operation(summary = "create", description = "Customer address is optional. When supplied, CEP automatically fills street, neighborhood, city and state through BrasilAPI; number and complement remain manual.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    CustomerResponse create(CreateCustomerRequest request);

    @Operation(summary = "find")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    CustomerResponse find(Integer id, Authentication authentication);

    @Operation(summary = "update", description = "When an address is supplied, its CEP is looked up through BrasilAPI. Number and complement remain manual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    CustomerResponse update(Integer id, UpdateCustomerRequest request, Authentication authentication);

    @Operation(summary = "delete")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful response")
    })
    void delete(Integer id);

    @Operation(summary = "anonymize")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful response")
    })
    void anonymize(Integer id, Authentication authentication);

    @Operation(summary = "geofences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<CustomerGeofenceResponse> geofences(Integer id, Authentication authentication);

    @Operation(summary = "addGeofence")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    CustomerGeofenceResponse addGeofence(Integer id, CreateCustomerGeofenceRequest request, Authentication authentication);

    @Operation(summary = "removeGeofence")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful response")
    })
    void removeGeofence(Integer id, Integer geofenceId, Authentication authentication);

    @Operation(summary = "loyalty")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    LoyaltyAccountResponse loyalty(Integer id, Authentication authentication);

}
