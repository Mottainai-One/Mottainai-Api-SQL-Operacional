package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.CompanyControllerApi;
import com.institutojf.mottainai.dto.request.CreateCompanyRequest;
import com.institutojf.mottainai.dto.request.UpdateCompanyRequest;
import com.institutojf.mottainai.dto.response.CompanyResponse;
import com.institutojf.mottainai.service.CompanyService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@PreAuthorize("hasRole('ADMINISTRATOR')")
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController implements CompanyControllerApi {

    private final CompanyService companyService;

    @Override
    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse company = companyService.create(request);
        URI location = URI.create("/api/v1/companies/" + company.id());
        return ResponseEntity.created(location).body(company);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<CompanyResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(companyService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        companyService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
