package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateEmployeeRequest;
import com.institutojf.mottainai.dto.request.EmployeeStatusRequest;
import com.institutojf.mottainai.dto.request.UpdateEmployeeRequest;
import com.institutojf.mottainai.dto.response.EmployeeCancelRequestResponse;
import com.institutojf.mottainai.dto.response.EmployeeResponse;
import com.institutojf.mottainai.dto.response.EmployeeShiftResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Employees", description = "Manage employees within the authenticated company")
public interface EmployeeControllerApi {

    @Operation(summary = "Create an inactive employee and send a password invitation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Store or role not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CPF or email conflict, or database constraint violation", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Employee saved inactive but invitation delivery failed; resend invitation", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<EmployeeResponse> create(CreateEmployeeRequest request, Authentication authentication);

    @Operation(summary = "List employees of the authenticated store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store employees found", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator or manager role required")
    })
    ResponseEntity<List<EmployeeResponse>> listStore(Authentication authentication);

    @Operation(summary = "List employees of the authenticated company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company employees found", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required")
    })
    ResponseEntity<List<EmployeeResponse>> listCompany(Authentication authentication);

    @Operation(summary = "Get an employee in the allowed scope")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<EmployeeResponse> find(Integer id, Authentication authentication);

    @Operation(summary = "Update employee registration data without changing CPF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Employee or role not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email conflict, or database constraint violation", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Employee data saved but new invitation delivery failed", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<EmployeeResponse> update(Integer id, UpdateEmployeeRequest request, Authentication authentication);

    @Operation(summary = "Logically delete an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Employee deactivated and logically deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete own access", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id, Authentication authentication);

    @Operation(summary = "Enable or disable employee access")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee access updated", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status change", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<EmployeeResponse> changeStatus(Integer id, EmployeeStatusRequest request, Authentication authentication);

    @Operation(summary = "Send a new invitation to an inactive employee who has not set a password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation registered; employee returned", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Employee already has a password or is active", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Invitation delivery failed; employee remains inactive and can be invited again", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<EmployeeResponse> resendInvitation(Integer id, Authentication authentication);

    @Operation(summary = "List employee POS shifts in a period of at most six months")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee shifts found", content = @Content(schema = @Schema(implementation = EmployeeShiftResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<EmployeeShiftResponse>> shifts(Integer id, LocalDateTime from, LocalDateTime to, Authentication authentication);

    @Operation(summary = "List employee cancellation requests in a period of at most six months")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancellation requests found", content = @Content(schema = @Schema(implementation = EmployeeCancelRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Employee not found in the allowed scope", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<EmployeeCancelRequestResponse>> cancelRequests(Integer id, LocalDateTime from, LocalDateTime to, Authentication authentication);

}
