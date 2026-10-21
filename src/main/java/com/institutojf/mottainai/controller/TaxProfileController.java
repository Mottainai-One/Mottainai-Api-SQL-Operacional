package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.TaxProfileControllerApi;
import com.institutojf.mottainai.dto.request.TaxProfileRequest;
import com.institutojf.mottainai.dto.response.TaxProfileResponse;
import com.institutojf.mottainai.service.TaxProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaxProfileController implements TaxProfileControllerApi {

    private final TaxProfileService taxProfileService;

    @Override
    @GetMapping("/tax-profiles")
    public ResponseEntity<Page<TaxProfileResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(taxProfileService.findAll(pageable));
    }

    @Override
    @GetMapping("/tax-profiles/{id}")
    public ResponseEntity<TaxProfileResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(taxProfileService.findById(id));
    }

    @Override
    @PostMapping("/tax-profiles")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<TaxProfileResponse> create(@Valid @RequestBody TaxProfileRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taxProfileService.create(request, authentication.getName()));
    }

    @Override
    @PutMapping("/tax-profile/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<TaxProfileResponse> update(@PathVariable Integer id, @Valid @RequestBody TaxProfileRequest request, Authentication authentication) {
        return ResponseEntity.ok(taxProfileService.update(id, request, authentication.getName()));
    }

    @Override
    @DeleteMapping("/tax-profiles/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        taxProfileService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
