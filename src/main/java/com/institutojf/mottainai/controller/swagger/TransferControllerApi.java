package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateTransferRequest;
import com.institutojf.mottainai.dto.request.UpdateTransferStatusRequest;
import com.institutojf.mottainai.dto.response.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Transfer", description = "API for Transfer")
public interface TransferControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<TransferResponse> findAll(Integer storeId, LocalDateTime from, LocalDateTime to, Authentication authentication);

    @Operation(summary = "findById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    TransferResponse findById(Integer id, Authentication authentication);

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    TransferResponse create(CreateTransferRequest request, String idempotencyKey, Authentication authentication);

    @Operation(summary = "updateStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    TransferResponse updateStatus(Integer id, UpdateTransferStatusRequest request, Authentication authentication);

    @Operation(summary = "receive")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    TransferResponse receive(Integer id, String idempotencyKey, Authentication authentication);

}
