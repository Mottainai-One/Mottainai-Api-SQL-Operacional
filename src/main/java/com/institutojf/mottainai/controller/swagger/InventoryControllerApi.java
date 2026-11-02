package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateInventoryMovementRequest;
import com.institutojf.mottainai.dto.request.CreateInventoryRequest;
import com.institutojf.mottainai.dto.request.UpdateInventoryRequest;
import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.dto.response.InventoryResponse;
import com.institutojf.mottainai.handler.ApiError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

@Tag(name = "Inventory", description = "API for managing inventory")
public interface InventoryControllerApi {

    @Operation(summary = "Find all inventory entries")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory entries found", content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    })
    ResponseEntity<List<InventoryResponse>> findAll(Integer storeId, Authentication authentication);

    @Operation(summary = "Find an inventory entry by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory entry found", content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<InventoryResponse> findById(Integer id, Authentication authentication);

    @Operation(summary = "Create an inventory entry or reactivate an existing deleted entry", description = "Reactivation preserves the previous balance and management settings; the POST settings are ignored in that case")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted inventory entry reactivated with reactivated=true", content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "201", description = "Inventory entry created", content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Store or batch not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Inventory entry already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<InventoryResponse> create(@Valid CreateInventoryRequest request, Authentication authentication);

    @Operation(summary = "Update an inventory entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory entry updated", content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<InventoryResponse> update(Integer id, @Valid UpdateInventoryRequest request, Authentication authentication);

    @Operation(summary = "Deactivate an inventory entry")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inventory entry deactivated"),
            @ApiResponse(responseCode = "404", description = "Inventory not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> deactivate(Integer id, Authentication authentication);

    @Operation(summary = "Find inventory entries by barcode")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory entries found", content = @Content(schema = @Schema(implementation = InventoryResponse.class)))
    })
    ResponseEntity<List<InventoryResponse>> findByBarcode(String barcode, Integer storeId, Authentication authentication);

    @Operation(summary = "Find inventory entries expiring within a period")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory entries found", content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<InventoryResponse>> findExpiring(Integer storeId, @Min(0) int days, Authentication authentication);

    @Operation(summary = "Find movements for an inventory entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory movements found", content = @Content(schema = @Schema(implementation = InventoryMovementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range; the maximum range is six months", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<InventoryMovementResponse>> findMovements(Integer id, @Parameter(description = "Start of the movement date range (ISO date-time)", required = true) LocalDateTime from, @Parameter(description = "End of the movement date range (ISO date-time), at most six months after from", required = true) LocalDateTime to, Authentication authentication);

    @Operation(summary = "Create an inventory movement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory movement created", content = @Content(schema = @Schema(implementation = InventoryMovementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient inventory balance", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<InventoryMovementResponse> createMovement(Integer id, @Valid CreateInventoryMovementRequest request, String idempotencyKey, Authentication authentication);

}
