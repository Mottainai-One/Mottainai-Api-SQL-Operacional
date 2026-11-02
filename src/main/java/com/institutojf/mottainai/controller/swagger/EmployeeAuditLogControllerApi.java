package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.AuditLogResponse;
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

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Employee audit logs", description = "Audit history of employee actions")
public interface EmployeeAuditLogControllerApi {

    @Operation(summary = "List an employee's audit history for a period of at most six months")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit records found", content = @Content(schema = @Schema(implementation = AuditLogResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<AuditLogResponse>> findByEmployee(Integer id, @Parameter(description = "Start of the audit date range (ISO date-time)", required = true) LocalDateTime from, @Parameter(description = "End of the audit date range (ISO date-time), at most six months after from", required = true) LocalDateTime to, Authentication authentication);
}
