package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@Tag(name = "Loyalty", description = "Customer sustainability count")
public interface LoyaltyControllerApi {

    @Operation(summary = "Get the authenticated customer's sustainability count")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count found", content = @Content(schema = @Schema(implementation = LoyaltyAccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer count not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LoyaltyAccountResponse getBalance(Authentication authentication);
}