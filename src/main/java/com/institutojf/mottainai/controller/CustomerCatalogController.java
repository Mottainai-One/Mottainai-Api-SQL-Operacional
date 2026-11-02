package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.dto.response.CustomerCatalogPromotionResponse;
import com.institutojf.mottainai.dto.response.CustomerCatalogStoreResponse;
import com.institutojf.mottainai.service.CustomerCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-catalog")
@RequiredArgsConstructor
public class CustomerCatalogController {
    private final CustomerCatalogService catalogService;

    @GetMapping("/stores")
    public Page<CustomerCatalogStoreResponse> stores(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String query,
            @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm) {
        return catalogService.stores(page, size, query, latitude, longitude, radiusKm);
    }

    @GetMapping("/stores/{id}")
    public CustomerCatalogStoreResponse store(@PathVariable Integer id) {
        return catalogService.store(id);
    }

    @GetMapping("/promotions")
    public Page<CustomerCatalogPromotionResponse> promotions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) String query) {
        return catalogService.promotions(page, size, storeId, query);
    }

    @GetMapping("/promotions/{id}")
    public CustomerCatalogPromotionResponse promotion(@PathVariable Integer id) {
        return catalogService.promotion(id);
    }
}
