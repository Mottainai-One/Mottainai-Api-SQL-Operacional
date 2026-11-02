package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.AddressControllerApi;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.dto.response.AddressResponse;
import com.institutojf.mottainai.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController implements AddressControllerApi {

    private final AddressService addressService;

    @Override
    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = addressService.create(request);
        URI location = URI.create("/api/v1/addresses/" + address.id());
        return ResponseEntity.created(location).body(address);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<AddressResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(addressService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(addressService.findById(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(addressService.update(id, request));
    }
}
