package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreatePromotionRequest;
import com.institutojf.mottainai.dto.request.UpdatePromotionRequest;
import com.institutojf.mottainai.dto.response.PromotionResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

import java.util.List;

@Tag(name = "Promotions", description = "API for managing promotions")
public interface PromotionControllerApi {

    @Operation(summary = "Find promotions by store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotions found", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<PromotionResponse> getPromotionsByStore(Integer storeId, Authentication authentication);

    @Operation(summary = "Find a promotion by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion found", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionResponse getPromotionById(Integer id, Authentication authentication);

    @Operation(summary = "Create a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promotion created", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionResponse createPromotion(CreatePromotionRequest request, Authentication authentication);

    @Operation(summary = "Update a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion updated", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionResponse updatePromotion(Integer id, UpdatePromotionRequest request, Authentication authentication);

    @Operation(summary = "Delete a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Promotion deleted"),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    void deletePromotion(Integer id, Authentication authentication);

    @Operation(summary = "Approve a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion approved", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionResponse approvePromotion(Integer id, Authentication authentication);

    @Operation(summary = "Reject a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion rejected", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionResponse rejectPromotion(Integer id, Authentication authentication);

    @Operation(summary = "Cancel a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion cancelled", content = @Content(schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionResponse cancelPromotion(Integer id, Authentication authentication);

}
