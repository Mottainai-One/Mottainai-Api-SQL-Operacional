package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateSuggestedActionRequest;
import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "AlertSuggestedAction", description = "API for AlertSuggestedAction")
public interface AlertSuggestedActionControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<SuggestedActionResponse> findAll(Integer alertId, Authentication authentication);

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    SuggestedActionResponse create(Integer alertId, CreateSuggestedActionRequest request, Authentication authentication);

}
