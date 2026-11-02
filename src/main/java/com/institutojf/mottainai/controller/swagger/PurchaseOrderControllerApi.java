package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.request.UpdatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.request.UpdatePurchaseOrderStatusRequest;
import com.institutojf.mottainai.dto.response.PurchaseOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "PurchaseOrder", description = "API for PurchaseOrder")
public interface PurchaseOrderControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<PurchaseOrderResponse> findAll(Integer storeId, LocalDateTime from, LocalDateTime to, Authentication authentication);

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    PurchaseOrderResponse create(CreatePurchaseOrderRequest request, String idempotencyKey, Authentication authentication);

    @Operation(summary = "findById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    PurchaseOrderResponse findById(Integer id, Authentication authentication);

    @Operation(summary = "update")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    PurchaseOrderResponse update(Integer id, UpdatePurchaseOrderRequest request, Authentication authentication);

    @Operation(summary = "updateStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    PurchaseOrderResponse updateStatus(Integer id, UpdatePurchaseOrderStatusRequest request, Authentication authentication);

    @Operation(summary = "delete")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful response")
    })
    void delete(Integer id, Authentication authentication);

}
