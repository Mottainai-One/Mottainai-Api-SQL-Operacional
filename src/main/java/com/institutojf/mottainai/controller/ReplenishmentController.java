package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.ReplenishmentControllerApi;
import com.institutojf.mottainai.dto.request.CreateReplenishmentExecutionRequest;
import com.institutojf.mottainai.dto.response.ReplenishmentExecutionResponse;
import com.institutojf.mottainai.dto.response.ReplenishmentPreListResponse;
import com.institutojf.mottainai.service.ReplenishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/replenishments")
@RequiredArgsConstructor
public class ReplenishmentController implements ReplenishmentControllerApi {

    private final ReplenishmentService service;

    @Override
    @PostMapping("/pre-lists")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplenishmentPreListResponse generate(@RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return service.generate(key, auth);
    }

    @Override
    @GetMapping("/pre-lists")
    public List<ReplenishmentPreListResponse> pending(@RequestParam(required = false) Integer storeId, Authentication auth) {
        return service.findPending(storeId, auth);
    }

    @Override
    @GetMapping("/pre-lists/{id}")
    public ReplenishmentPreListResponse preList(@PathVariable Integer id, Authentication auth) {
        return service.findPreList(id, auth);
    }

    @Override
    @PostMapping("/executions")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplenishmentExecutionResponse execute(@Valid @RequestBody CreateReplenishmentExecutionRequest request, @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return service.execute(request, key, auth);
    }

    @Override
    @GetMapping("/executions")
    public List<ReplenishmentExecutionResponse> executions(@RequestParam(required = false) Integer storeId, @RequestParam(required = false) LocalDateTime from, @RequestParam(required = false) LocalDateTime to, Authentication auth) {
        return service.findExecutions(storeId, from, to, auth);
    }

    @Override
    @GetMapping("/executions/{id}")
    public ReplenishmentExecutionResponse execution(@PathVariable Integer id, Authentication auth) {
        return service.findExecution(id, auth);
    }

}
