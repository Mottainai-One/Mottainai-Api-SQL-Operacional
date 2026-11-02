package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.SupplierControllerApi;
import com.institutojf.mottainai.dto.request.CreateSupplierRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierRequest;
import com.institutojf.mottainai.dto.request.LinkProductToSupplierRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierProductRequest;
import com.institutojf.mottainai.dto.response.SupplierResponse;
import com.institutojf.mottainai.dto.response.SupplierProductResponse;
import com.institutojf.mottainai.dto.response.SupplierPurchaseHistoryResponse;
import com.institutojf.mottainai.service.SupplierService;
import com.institutojf.mottainai.service.SupplierProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController implements SupplierControllerApi {

    private final SupplierService supplierService;

    private final SupplierProductService supplierProductService;

    @Override
    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse supplier = supplierService.create(request);
        URI location = URI.create("/api/v1/suppliers/" + supplier.id());
        return ResponseEntity.created(location).body(supplier);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<SupplierResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(supplierService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{id}/history")
    public ResponseEntity<List<SupplierPurchaseHistoryResponse>> findHistory(@PathVariable Integer id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(supplierService.findPurchaseHistory(id, from, to));
    }

    @Override
    @GetMapping("/{id}/products")
    public ResponseEntity<Page<SupplierProductResponse>> findProducts(@PathVariable Integer id, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(supplierProductService.findBySupplier(id, pageable));
    }

    @Override
    @PostMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<SupplierProductResponse> linkProduct(@PathVariable Integer id, @Valid @RequestBody LinkProductToSupplierRequest request, Authentication authentication) {
        SupplierProductResponse link = supplierProductService.link(id, request.productId(), request.supplierCode(), request.purchasePrice(), request.leadTime(), authentication);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + id + "/products/" + request.productId()))
            .body(link);
    }

    @Override
    @PutMapping("/{id}/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<SupplierProductResponse> updateProductLink(@PathVariable Integer id, @PathVariable Integer productId, @Valid @RequestBody UpdateSupplierProductRequest request, Authentication authentication) {
        return ResponseEntity.ok(supplierProductService.update(id, productId, request, authentication));
    }

    @Override
    @DeleteMapping("/{id}/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<Void> deactivateProductLink(@PathVariable Integer id, @PathVariable Integer productId, Authentication authentication) {
        supplierProductService.deactivate(id, productId, authentication);
        return ResponseEntity.noContent().build();
    }

}
