package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.LoyaltyControllerApi;
import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.security.CustomerAccess;
import com.institutojf.mottainai.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/loyalty")
@RequiredArgsConstructor
public class LoyaltyController implements LoyaltyControllerApi {

    private final LoyaltyService loyaltyService;

    private final CustomerAccess customerAccess;

    @Override
    @GetMapping("/balance")
    public LoyaltyAccountResponse getBalance(Authentication authentication) {
        return loyaltyService.getLoyaltyAccount(customerAccess.currentCustomer(authentication).getId());
    }
}