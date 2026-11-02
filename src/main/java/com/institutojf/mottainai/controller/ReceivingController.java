package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.ReceivingControllerApi;
import com.institutojf.mottainai.dto.request.CreateReceivingRequest;
import com.institutojf.mottainai.dto.request.UpdateReceivingStatusRequest;
import com.institutojf.mottainai.dto.response.ReceivingResponse;
import com.institutojf.mottainai.service.ReceivingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/receivings")
@RequiredArgsConstructor
public class ReceivingController implements ReceivingControllerApi {

    private final ReceivingService receivingService;

    @Override
    @GetMapping
    public List<ReceivingResponse> findAll(@RequestParam(required = false) Integer storeId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return receivingService.findAll(storeId, from, to, authentication);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceivingResponse create(@Valid @RequestBody CreateReceivingRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        return receivingService.create(request, idempotencyKey, authentication);
    }

    @Override
    @GetMapping("/{id}")
    public ReceivingResponse findById(@PathVariable Integer id, Authentication authentication) {
        return receivingService.getById(id, authentication);
    }

    @Override
    @PatchMapping("/{id}/status")
    public ReceivingResponse updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateReceivingStatusRequest request, Authentication authentication) {
        return receivingService.updateStatus(id, request.status(), authentication);
    }

}
