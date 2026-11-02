package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.AuditLogControllerApi;
import com.institutojf.mottainai.dto.response.AuditLogResponse;
import com.institutojf.mottainai.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController implements AuditLogControllerApi {

    private final AuditLogService auditLogService;

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<AuditLogResponse>> findAll( @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, @RequestParam(required = false) String tableAffected, @RequestParam(required = false) String operation, Authentication authentication) {
        return ResponseEntity.ok(auditLogService.findAll(from, to, tableAffected, operation, authentication));
    }

}
