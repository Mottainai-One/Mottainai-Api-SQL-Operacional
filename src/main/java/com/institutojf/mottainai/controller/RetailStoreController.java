package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.RetailStoreControllerApi;
import com.institutojf.mottainai.dto.request.CreateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.UpdateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.UpdateActiveStatusRequest;
import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.service.RetailStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class RetailStoreController implements RetailStoreControllerApi {

    private final RetailStoreService retailStoreService;

    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<RetailStoreResponse> create(@Valid @RequestBody CreateRetailStoreRequest request) {
        RetailStoreResponse store = retailStoreService.create(request);
        URI location = URI.create("/api/v1/stores/" + store.id());
        return ResponseEntity.created(location).body(store);
    }

    @Override
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<RetailStoreResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(retailStoreService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or (hasRole('MANAGER') and @retailStoreAccess.isCurrentUserStore(#id, authentication))")
    public ResponseEntity<RetailStoreResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(retailStoreService.findById(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or (hasRole('MANAGER') and @retailStoreAccess.isCurrentUserStore(#id, authentication))")
    public ResponseEntity<RetailStoreResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateRetailStoreRequest request) {
        return ResponseEntity.ok(retailStoreService.update(id, request));
    }

    @Override
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<RetailStoreResponse> updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateActiveStatusRequest request) {
        return ResponseEntity.ok(retailStoreService.updateStatus(id, request.active()));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        retailStoreService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
