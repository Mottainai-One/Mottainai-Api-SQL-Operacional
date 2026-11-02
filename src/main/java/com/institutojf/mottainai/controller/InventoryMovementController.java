package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.InventoryMovementControllerApi;
import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.service.InventoryMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryMovementController implements InventoryMovementControllerApi {

    private final InventoryMovementService inventoryMovementService;

    @Override
    @GetMapping("/api/v1/inventory-movements")
    public ResponseEntity<List<InventoryMovementResponse>> findByStore(@RequestParam(required = false) Integer storeId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return ResponseEntity.ok(inventoryMovementService.findByStore(storeId, from, to, authentication));
    }

}
