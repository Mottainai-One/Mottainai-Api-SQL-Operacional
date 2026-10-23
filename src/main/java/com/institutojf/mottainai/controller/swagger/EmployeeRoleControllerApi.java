package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.EmployeeRoleResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Employee Roles", description = "Consult employee role data.")
public interface EmployeeRoleControllerApi {

    @Operation(summary = "List active employee roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles found", content = @Content(schema = @Schema(implementation = EmployeeRoleResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    ResponseEntity<List<EmployeeRoleResponse>> findAll();

    @Operation(summary = "Get an employee role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found", content = @Content(schema = @Schema(implementation = EmployeeRoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<EmployeeRoleResponse> findById(Integer id);
}
