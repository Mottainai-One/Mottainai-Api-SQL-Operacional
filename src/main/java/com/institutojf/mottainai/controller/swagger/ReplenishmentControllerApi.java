package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateReplenishmentExecutionRequest;
import com.institutojf.mottainai.dto.response.ReplenishmentExecutionResponse;
import com.institutojf.mottainai.dto.response.ReplenishmentPreListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Replenishment", description = "API for Replenishment")
public interface ReplenishmentControllerApi {

    @Operation(summary = "generate")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    ReplenishmentPreListResponse generate(String key, Authentication auth);

    @Operation(summary = "pending")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<ReplenishmentPreListResponse> pending(Integer storeId, Authentication auth);

    @Operation(summary = "preList")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ReplenishmentPreListResponse preList(Integer id, Authentication auth);

    @Operation(summary = "execute")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    ReplenishmentExecutionResponse execute(CreateReplenishmentExecutionRequest request, String key, Authentication auth);

    @Operation(summary = "executions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<ReplenishmentExecutionResponse> executions(Integer storeId, LocalDateTime from, LocalDateTime to, Authentication auth);

    @Operation(summary = "execution")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ReplenishmentExecutionResponse execution(Integer id, Authentication auth);

}
