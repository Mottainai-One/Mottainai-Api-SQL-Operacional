package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.request.UpdateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
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

@Tag(name = "Subscription Plans", description = "API for managing subscription plans")
public interface SubscriptionPlanControllerApi {

    @Operation(summary = "Create a subscription plan")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subscription plan created", content = @Content(schema = @Schema(implementation = SubscriptionPlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Name already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SubscriptionPlanResponse> create(CreateSubscriptionPlanRequest request);

    @Operation(summary = "Find all subscription plans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription plan list found", content = @Content(schema = @Schema(implementation = SubscriptionPlanResponse.class)))
    })
    ResponseEntity<Page<SubscriptionPlanResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find a subscription plan by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription plan found", content = @Content(schema = @Schema(implementation = SubscriptionPlanResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SubscriptionPlanResponse> findById(Integer id);

    @Operation(summary = "Update a subscription plan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription plan updated", content = @Content(schema = @Schema(implementation = SubscriptionPlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SubscriptionPlanResponse> update(Integer id, UpdateSubscriptionPlanRequest request);

    @Operation(summary = "Logically delete a subscription plan")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subscription plan logically deleted"),
            @ApiResponse(responseCode = "400", description = "Subscription plan cannot be logically deleted", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id);

}
