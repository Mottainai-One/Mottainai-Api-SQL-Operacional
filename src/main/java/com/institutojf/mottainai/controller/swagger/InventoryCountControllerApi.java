package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.request.CreateInventoryCountRequest;
import com.institutojf.mottainai.dto.request.UpdateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.response.InventoryCountItemResponse;
import com.institutojf.mottainai.dto.response.InventoryCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "InventoryCount", description = "API for InventoryCount")
public interface InventoryCountControllerApi {

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<InventoryCountResponse> create(CreateInventoryCountRequest request, Authentication authentication);

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<InventoryCountResponse>> findAll(Authentication authentication);

    @Operation(summary = "findById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<InventoryCountResponse> findById(Long id, Authentication authentication);

    @Operation(summary = "addItem")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<InventoryCountItemResponse> addItem(Long id, CreateInventoryCountItemRequest request, Authentication authentication);

    @Operation(summary = "updateItem")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<InventoryCountItemResponse> updateItem(Long id, Long itemId, UpdateInventoryCountItemRequest request, Authentication authentication);

    @Operation(summary = "deleteItem")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<Void> deleteItem(Long id, Long itemId, Authentication authentication);

    @Operation(summary = "finish")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<InventoryCountResponse> finish(Long id, String idempotencyKey, Authentication authentication);

}
