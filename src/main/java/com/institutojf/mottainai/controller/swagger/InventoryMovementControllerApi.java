package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Inventory movements", description = "Inventory movement ledger")
public interface InventoryMovementControllerApi {

    @Operation(summary = "Find inventory movements for a store within at most six months")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movements found", content = @Content(schema = @Schema(implementation = InventoryMovementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or store access", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<InventoryMovementResponse>> findByStore( @Parameter(description = "Store to query; required for administrators") Integer storeId, @Parameter(description = "Start of the movement date range (ISO date-time)", required = true) LocalDateTime from, @Parameter(description = "End of the movement date range (ISO date-time), at most six months after from", required = true) LocalDateTime to, Authentication authentication);

}
