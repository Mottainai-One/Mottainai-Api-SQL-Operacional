package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.UpdateActiveStatusRequest;
import com.institutojf.mottainai.dto.response.BatchResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Batches", description = "API for managing batches")
public interface BatchControllerApi {

    @Operation(summary = "Find all batches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batches found", content = @Content(schema = @Schema(implementation = BatchResponse.class)))
    })
    ResponseEntity<List<BatchResponse>> findAll(@RequestParam(required = false) Integer storeId, Authentication authentication);

    @Operation(summary = "Find a batch by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch found", content = @Content(schema = @Schema(implementation = BatchResponse.class))),
            @ApiResponse(responseCode = "404", description = "Batch not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<BatchResponse> findById(Integer id, Authentication authentication);

    @Operation(summary = "Change batch active status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<BatchResponse> updateStatus(Integer id, UpdateActiveStatusRequest request, Authentication authentication);

}
