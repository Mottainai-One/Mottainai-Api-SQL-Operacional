package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.DonationControllerApi;
import com.institutojf.mottainai.dto.request.CreateDonationRequest;
import com.institutojf.mottainai.dto.request.UpdateDonationStatusRequest;
import com.institutojf.mottainai.dto.response.DonationResponse;
import com.institutojf.mottainai.service.DonationService;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
public class DonationController implements DonationControllerApi {

    private final DonationService donationService;

    @Override
    @GetMapping
    public List<DonationResponse> findAll(@RequestParam(required = false) Integer storeId, @RequestParam LocalDateTime from, @RequestParam LocalDateTime to, Authentication auth) {
        return donationService.findAll(storeId, from, to, auth);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse create(@Valid @RequestBody CreateDonationRequest request, @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return donationService.create(request, key, auth);
    }

    @Override
    @GetMapping("/{id}")
    public DonationResponse findById(@PathVariable Integer id, Authentication auth) {
        return donationService.findById(id, auth);
    }

    @Override
    @PatchMapping("/{id}/status")
    public DonationResponse updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateDonationStatusRequest request, Authentication auth) {
        return donationService.updateStatus(id, request, auth);
    }

}
