package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "AuditLog", description = "API for AuditLog")
public interface AuditLogControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<List<AuditLogResponse>> findAll( LocalDateTime from, LocalDateTime to, String tableAffected, String operation, Authentication authentication);

}
