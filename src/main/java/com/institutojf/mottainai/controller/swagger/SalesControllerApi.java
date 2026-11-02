package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.SalesSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Sales", description = "Store sales summaries")
public interface SalesControllerApi {

    @Operation(summary = "Get the current month's completed sales for the authenticated store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly sales summary",
                    content = @Content(schema = @Schema(implementation = SalesSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Store access denied")
    })
    ResponseEntity<SalesSummaryResponse> currentMonth(
            @Parameter(description = "Store to summarize; required for administrators") Integer storeId,
            Authentication authentication);
}
