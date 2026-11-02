package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.SystemEventResponse;
import com.institutojf.mottainai.dto.response.SystemJobResponse;
import com.institutojf.mottainai.dto.response.SystemLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "SystemMonitoring", description = "API for SystemMonitoring")
public interface SystemMonitoringControllerApi {

    @Operation(summary = "events")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<SystemEventResponse> events(LocalDateTime from, LocalDateTime to, String status, Authentication auth);

    @Operation(summary = "retry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    SystemEventResponse retry(Long id, Authentication auth);

    @Operation(summary = "logs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<SystemLogResponse> logs(LocalDateTime from, LocalDateTime to, String level, Authentication auth);

    @Operation(summary = "jobs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<SystemJobResponse> jobs(LocalDateTime from, LocalDateTime to, Boolean success, Authentication auth);

}
