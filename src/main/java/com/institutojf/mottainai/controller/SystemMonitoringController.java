package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.SystemMonitoringControllerApi;
import com.institutojf.mottainai.dto.response.SystemEventResponse;
import com.institutojf.mottainai.dto.response.SystemJobResponse;
import com.institutojf.mottainai.dto.response.SystemLogResponse;
import com.institutojf.mottainai.service.SystemMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
public class SystemMonitoringController implements SystemMonitoringControllerApi {

    private final SystemMonitoringService service;

    @Override
    @GetMapping("/events")
    public List<SystemEventResponse> events(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to, @RequestParam(required = false) String status, Authentication auth) {
        return service.events(from, to, status, auth);
    }

    @Override
    @PostMapping("/events/{id}/retry")
    public SystemEventResponse retry(@PathVariable Long id, Authentication auth) {
        return service.retry(id, auth);
    }

    @Override
    @GetMapping("/logs")
    public List<SystemLogResponse> logs(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to, @RequestParam(required = false) String level, Authentication auth) {
        return service.logs(from, to, level, auth);
    }

    @Override
    @GetMapping("/jobs")
    public List<SystemJobResponse> jobs(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to, @RequestParam(required = false) Boolean success, Authentication auth) {
        return service.jobs(from, to, success, auth);
    }

}
