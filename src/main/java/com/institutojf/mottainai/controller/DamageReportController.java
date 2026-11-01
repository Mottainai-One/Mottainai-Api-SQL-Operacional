package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.request.CreateDamageReportRequest;
import com.institutojf.mottainai.dto.response.DamageReportResponse;
import com.institutojf.mottainai.service.DamageReportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/damages") @RequiredArgsConstructor
public class DamageReportController {
    private final DamageReportService service;
    @GetMapping public List<DamageReportResponse> findAll(Authentication authentication) {
        return service.findAll(authentication);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public DamageReportResponse create(@Valid @RequestBody CreateDamageReportRequest request, Authentication authentication) {
        return service.create(request, authentication);
    }
}
