package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.UpdateSystemRuleRequest;
import com.institutojf.mottainai.dto.response.SystemRuleResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

@Tag(name = "System rules", description = "Global application rules")
public interface SystemRuleControllerApi {

    @Operation(summary = "List all global and category-specific system rules")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System rules listed", content = @Content(schema = @Schema(implementation = SystemRuleResponse.class)))
    })
    ResponseEntity<List<SystemRuleResponse>> findAll();

    @Operation(summary = "Update a system rule and record the change in the audit log")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System rule updated", content = @Content(schema = @Schema(implementation = SystemRuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rule value", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "System rule not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Rule key is ambiguous across categories", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<SystemRuleResponse> update(String key, @Parameter(description = "Required only when the key exists in multiple categories") String category, UpdateSystemRuleRequest request, Authentication authentication);

}
