package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreatePromotionItemRequest;
import com.institutojf.mottainai.dto.request.UpdatePromotionItemRequest;
import com.institutojf.mottainai.dto.response.PromotionItemResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

import java.util.List;

@Tag(name = "Promotion Items", description = "API for managing promotion items")
public interface PromotionItemControllerApi {

    @Operation(summary = "Find items for a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion items found", content = @Content(schema = @Schema(implementation = PromotionItemResponse.class)))
    })
    List<PromotionItemResponse> getItemsByPromotion(Integer promotionId, Authentication authentication);

    @Operation(summary = "Add an item to a promotion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promotion item created", content = @Content(schema = @Schema(implementation = PromotionItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or product already belongs to the promotion", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Promotion or product not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PromotionItemResponse createPromotionItem(Integer promotionId, CreatePromotionItemRequest request, Authentication authentication);

    @Operation(summary = "Update a promotion item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    PromotionItemResponse updatePromotionItem(Integer promotionId, Integer id, UpdatePromotionItemRequest request, Authentication authentication);

    @Operation(summary = "Delete a promotion item")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Promotion item deleted"),
            @ApiResponse(responseCode = "404", description = "Promotion item not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    void deletePromotionItem(Integer promotionId, Integer id, Authentication authentication);

}
