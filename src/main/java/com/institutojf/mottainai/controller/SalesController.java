package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.SalesControllerApi;
import com.institutojf.mottainai.dto.response.SalesSummaryResponse;
import com.institutojf.mottainai.service.SalesSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesController implements SalesControllerApi {

    private final SalesSummaryService salesSummaryService;

    @Override
    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> currentMonth(
            @RequestParam(required = false) Integer storeId, Authentication authentication) {
        return ResponseEntity.ok(salesSummaryService.currentMonth(authentication, storeId));
    }
}
