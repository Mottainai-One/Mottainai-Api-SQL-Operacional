package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.TransferControllerApi;
import com.institutojf.mottainai.dto.request.CreateTransferRequest;
import com.institutojf.mottainai.dto.request.UpdateTransferStatusRequest;
import com.institutojf.mottainai.dto.response.TransferResponse;
import com.institutojf.mottainai.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController implements TransferControllerApi {

    private final TransferService transferService;

    @Override
    @GetMapping
    public List<TransferResponse> findAll(@RequestParam(required = false) Integer storeId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return transferService.findAll(storeId, from, to, authentication);
    }

    @Override
    @GetMapping("/{id}")
    public TransferResponse findById(@PathVariable Integer id, Authentication authentication) {
        return transferService.findById(id, authentication);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        return transferService.create(request, idempotencyKey, authentication);
    }

    @Override
    @PatchMapping("/{id}/status")
    public TransferResponse updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateTransferStatusRequest request, Authentication authentication) {
        return transferService.updateStatus(id, request, authentication);
    }

    @Override
    @PostMapping("/{id}/receive")
    public TransferResponse receive(@PathVariable Integer id, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        return transferService.receive(id, idempotencyKey, authentication);
    }

}
