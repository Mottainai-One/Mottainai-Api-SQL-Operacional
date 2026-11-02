package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.SystemRuleControllerApi;
import com.institutojf.mottainai.dto.request.UpdateSystemRuleRequest;
import com.institutojf.mottainai.dto.response.SystemRuleResponse;
import com.institutojf.mottainai.service.SystemRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-rules")
@RequiredArgsConstructor
public class SystemRuleController implements SystemRuleControllerApi {

    private final SystemRuleService systemRuleService;

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<SystemRuleResponse>> findAll() {
        return ResponseEntity.ok(systemRuleService.findAll());
    }

    @Override
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SystemRuleResponse> update(@PathVariable String key, @RequestParam(required = false) String category, @Valid @RequestBody UpdateSystemRuleRequest request, Authentication authentication) {
        return ResponseEntity.ok(systemRuleService.update(key, category, request, authentication.getName()));
    }

}
