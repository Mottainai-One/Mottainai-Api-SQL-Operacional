package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.CustomerControllerApi;
import com.institutojf.mottainai.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.dto.request.CreateCustomerRequest;
import com.institutojf.mottainai.dto.request.UpdateCustomerRequest;

import com.institutojf.mottainai.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.dto.response.CustomerPrivateDataResponse;
import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;

import com.institutojf.mottainai.security.CustomerAccess;
import com.institutojf.mottainai.service.CustomerService;
import com.institutojf.mottainai.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController implements CustomerControllerApi {

    private final CustomerService customerService;

    private final LoyaltyService loyaltyService;

    private final CustomerAccess customerAccess;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<CustomerResponse> findAll() {
        return customerService.findAll();
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','CUSTOMER')")
    public CustomerResponse find(@PathVariable Integer id, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        return customerService.find(id);
    }

    /** Staff-only retrieval of original PII; returns no data for inactive customers. */
    @GetMapping("/{id}/private-data")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public CustomerPrivateDataResponse privateData(@PathVariable Integer id) {
        return customerService.privateData(id);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','CUSTOMER')")
    public CustomerResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateCustomerRequest request, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        return customerService.update(id, request);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void delete(@PathVariable Integer id) {
        customerService.delete(id);
    }

    @Override
    @PostMapping("/{id}/anonymize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','CUSTOMER')")
    public void anonymize(@PathVariable Integer id, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        customerService.anonymize(id);
    }

    @Override
    @GetMapping("/{id}/geofences")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','CUSTOMER')")
    public List<CustomerGeofenceResponse> geofences(@PathVariable Integer id, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        return customerService.geofences(id);
    }

    @Override
    @PostMapping("/{id}/geofences")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerGeofenceResponse addGeofence(@PathVariable Integer id, @Valid @RequestBody CreateCustomerGeofenceRequest request, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        return customerService.addGeofence(id, request);
    }

    @Override
    @DeleteMapping("/{id}/geofences/{geofenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CUSTOMER')")
    public void removeGeofence(@PathVariable Integer id, @PathVariable Integer geofenceId, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        customerService.removeGeofence(id, geofenceId);
    }

    @Override
    @GetMapping("/{id}/loyalty")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','CUSTOMER')")
    public LoyaltyAccountResponse loyalty(@PathVariable Integer id, Authentication authentication) {
        customerAccess.checkAccess(authentication, id);
        return loyaltyService.getLoyaltyAccount(id);
    }

}
