package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.CustomerControllerApi;
import com.institutojf.mottainai.dto.request.CreateCustomerRequest;
import com.institutojf.mottainai.dto.request.UpdateCustomerRequest;
import com.institutojf.mottainai.dto.request.UpdateLoyaltyStatusRequest;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.dto.response.LoyaltyTransactionResponse;
import com.institutojf.mottainai.service.CustomerService;
import com.institutojf.mottainai.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController implements CustomerControllerApi {

    private final CustomerService customerService;

    private final LoyaltyService loyaltyService;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<CustomerResponse> findAll() {
        return customerService.findAll();
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public CustomerResponse find(@PathVariable Integer id) {
        return customerService.find(id);
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public CustomerResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateCustomerRequest request) {
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void anonymize(@PathVariable Integer id) {
        customerService.anonymize(id);
    }

    @Override
    @PatchMapping("/{id}/loyalty/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public LoyaltyAccountResponse updateLoyaltyStatus(@PathVariable Integer id, @Valid @RequestBody UpdateLoyaltyStatusRequest request) {
        return customerService.updateLoyaltyStatus(id, request);
    }

    @Override
    @GetMapping("/{id}/loyalty")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public LoyaltyAccountResponse loyalty(@PathVariable Integer id) {
        return loyaltyService.getLoyaltyAccount(id);
    }

    @Override
    @GetMapping("/{id}/loyalty/transactions")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<LoyaltyTransactionResponse> loyaltyTransactions(@PathVariable Integer id, @RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {
        return loyaltyService.getTransactions(id, from, to);
    }

}
