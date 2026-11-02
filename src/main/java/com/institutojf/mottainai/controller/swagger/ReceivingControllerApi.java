package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateReceivingRequest;
import com.institutojf.mottainai.dto.request.UpdateReceivingStatusRequest;
import com.institutojf.mottainai.dto.response.ReceivingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Receiving", description = "API for Receiving")
public interface ReceivingControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<ReceivingResponse> findAll(Integer storeId, LocalDateTime from, LocalDateTime to, Authentication authentication);

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    ReceivingResponse create(CreateReceivingRequest request, String idempotencyKey, Authentication authentication);

    @Operation(summary = "findById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ReceivingResponse findById(Integer id, Authentication authentication);

    @Operation(summary = "updateStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ReceivingResponse updateStatus(Integer id, UpdateReceivingStatusRequest request, Authentication authentication);

}
