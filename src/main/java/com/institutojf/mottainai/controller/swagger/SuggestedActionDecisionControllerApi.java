package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.ExecuteSuggestedActionRequest;
import com.institutojf.mottainai.dto.request.SuggestedActionDecisionRequest;
import com.institutojf.mottainai.dto.response.ExecuteSuggestedActionResponse;
import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;

@Tag(name = "SuggestedActionDecision", description = "API for SuggestedActionDecision")
public interface SuggestedActionDecisionControllerApi {

    @Operation(summary = "decide")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    SuggestedActionResponse decide(Integer id, SuggestedActionDecisionRequest request, Authentication authentication);

    @Operation(summary = "execute")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ExecuteSuggestedActionResponse execute(Integer id, ExecuteSuggestedActionRequest request, String idempotencyKey, Authentication authentication);

}
