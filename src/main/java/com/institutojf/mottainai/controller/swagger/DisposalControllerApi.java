package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateDisposalRequest;
import com.institutojf.mottainai.dto.response.DisposalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Disposal", description = "API for Disposal")
public interface DisposalControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<DisposalResponse> findAll(Integer storeId, String reason, LocalDateTime from, LocalDateTime to, Authentication auth);

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    DisposalResponse create(CreateDisposalRequest request, String key, Authentication auth);

    @Operation(summary = "findById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    DisposalResponse findById(Integer id, Authentication auth);

}
