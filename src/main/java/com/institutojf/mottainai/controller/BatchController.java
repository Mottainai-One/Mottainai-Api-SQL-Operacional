package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.BatchControllerApi;
import com.institutojf.mottainai.dto.request.UpdateActiveStatusRequest;
import com.institutojf.mottainai.dto.response.BatchResponse;
import com.institutojf.mottainai.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController implements BatchControllerApi {

    private final BatchService batchService;

    @Override
    @GetMapping
    public ResponseEntity<List<BatchResponse>> findAll(@RequestParam(required = false) Integer storeId, Authentication authentication) {
        return ResponseEntity.ok(batchService.findAll(storeId, authentication));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<BatchResponse> findById(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(batchService.findById(id, authentication));
    }

    @Override
    @PatchMapping("/{id}/status")
    public ResponseEntity<BatchResponse> updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateActiveStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(batchService.updateStatus(id, request.active(), authentication));
    }

}
