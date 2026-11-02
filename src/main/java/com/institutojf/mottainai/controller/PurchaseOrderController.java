package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.PurchaseOrderControllerApi;
import com.institutojf.mottainai.dto.request.CreatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.request.UpdatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.request.UpdatePurchaseOrderStatusRequest;
import com.institutojf.mottainai.dto.response.PurchaseOrderResponse;
import com.institutojf.mottainai.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController implements PurchaseOrderControllerApi {

    private final PurchaseOrderService purchaseOrderService;

    @Override
    @GetMapping
    public List<PurchaseOrderResponse> findAll(@RequestParam(required = false) Integer storeId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return purchaseOrderService.findAll(storeId, from, to, authentication);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public PurchaseOrderResponse create(@Valid @RequestBody CreatePurchaseOrderRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        return purchaseOrderService.create(request, idempotencyKey, authentication);
    }

    @Override
    @GetMapping("/{id}")
    public PurchaseOrderResponse findById(@PathVariable Integer id, Authentication authentication) {
        return purchaseOrderService.getById(id, authentication);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public PurchaseOrderResponse update(@PathVariable Integer id, @Valid @RequestBody UpdatePurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderService.update(id, request, authentication);
    }

    @Override
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public PurchaseOrderResponse updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdatePurchaseOrderStatusRequest request, Authentication authentication) {
        return purchaseOrderService.updateStatus(id, request.status(), authentication);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public void delete(@PathVariable Integer id, Authentication authentication) {
        purchaseOrderService.delete(id, authentication);
    }

}
