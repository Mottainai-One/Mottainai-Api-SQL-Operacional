package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateAlertRequest;
import com.institutojf.mottainai.dto.request.UpdateAlertStatusRequest;
import com.institutojf.mottainai.dto.response.AlertResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

import java.util.List;

@Tag(name = "Alerts", description = "API for managing alerts")
public interface AlertControllerApi {

    @Operation(summary = "Find alerts by store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerts found", content = @Content(schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<AlertResponse> getAlertsByStore(Integer storeId, Authentication authentication);

    @Operation(summary = "Find an alert by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert found", content = @Content(schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    AlertResponse getAlertById(Integer id, Authentication authentication);

    @Operation(summary = "Create an alert")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alert created", content = @Content(schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    AlertResponse createAlert(CreateAlertRequest request, Authentication authentication);

    @Operation(summary = "Update an alert status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert status updated", content = @Content(schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    AlertResponse updateStatus(Integer id, UpdateAlertStatusRequest request, Authentication authentication);

}
