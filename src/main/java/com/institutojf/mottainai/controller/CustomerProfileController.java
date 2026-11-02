package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.CustomerProfileControllerApi;
import com.institutojf.mottainai.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.service.CustomerAuthenticationService;
import com.institutojf.mottainai.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerProfileController implements CustomerProfileControllerApi {

    private final CustomerAuthenticationService authenticationService;

    private final CustomerService customerService;

    @Override
    @GetMapping("/client/auth/profile")
    public CustomerResponse clientProfile(Authentication authentication) {
        return authenticationService.profile(authentication);
    }

    @Override
    @PostMapping("/client/auth/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createClientProfile(
        @Valid @RequestBody CreateFirebaseCustomerProfileRequest request,
        @AuthenticationPrincipal Jwt token) {
        return customerService.createFirebaseProfile(request, token.getSubject(), token.getClaimAsString("email"));
    }

    @Override
    @PutMapping("/client/auth/profile")
    public CustomerResponse updateClientProfile(
        @Valid @RequestBody UpdateFirebaseCustomerProfileRequest request,
        @AuthenticationPrincipal Jwt token) {
        return customerService.updateFirebaseProfile(request, token.getSubject());
    }

}
