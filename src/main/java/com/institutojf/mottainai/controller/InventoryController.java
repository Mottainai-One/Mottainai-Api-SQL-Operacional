package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.InventoryControllerApi;
import com.institutojf.mottainai.dto.request.CreateInventoryMovementRequest;
import com.institutojf.mottainai.dto.request.CreateInventoryRequest;
import com.institutojf.mottainai.dto.request.UpdateInventoryRequest;
import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.dto.response.InventoryResponse;
import com.institutojf.mottainai.service.InventoryMovementService;
import com.institutojf.mottainai.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController implements InventoryControllerApi {

    private final InventoryService inventoryService;

    private final InventoryMovementService inventoryMovementService;

    @Override
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> findAll(@RequestParam(required = false) Integer storeId, Authentication authentication) {
        return ResponseEntity.ok(inventoryService.findAll(storeId, authentication));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> findById(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(inventoryService.findById(id, authentication));
    }

    @Override
    @PostMapping
    public ResponseEntity<InventoryResponse> create(@RequestBody CreateInventoryRequest request, Authentication authentication) {
        InventoryResponse inventory = inventoryService.create(request, authentication);
        if (inventory.reactivated()) {
            return ResponseEntity.ok(inventory);
        }
        return ResponseEntity.created(URI.create("/api/v1/inventory/" + inventory.id())).body(inventory);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<InventoryResponse> update(@PathVariable Integer id, @RequestBody UpdateInventoryRequest request, Authentication authentication) {
        return ResponseEntity.ok(inventoryService.update(id, request, authentication));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id, Authentication authentication) {
        inventoryService.deactivate(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<List<InventoryResponse>> findByBarcode(@PathVariable String barcode, @RequestParam(required = false) Integer storeId, Authentication authentication) {
        return ResponseEntity.ok(inventoryService.findByBarcode(barcode, storeId, authentication));
    }

    @Override
    @GetMapping("/expiring")
    public ResponseEntity<List<InventoryResponse>> findExpiring(@RequestParam(required = false) Integer storeId, @RequestParam(defaultValue = "30") int days, Authentication authentication) {
        return ResponseEntity.ok(inventoryService.findExpiring(storeId, days, authentication));
    }

    @Override
    @GetMapping("/{id}/movements")
    public ResponseEntity<List<InventoryMovementResponse>> findMovements(@PathVariable Integer id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, Authentication authentication) {
        return ResponseEntity.ok(inventoryMovementService.findByInventory(id, from, to, authentication));
    }

    @Override
    @PostMapping("/{id}/movements")
    public ResponseEntity<InventoryMovementResponse> createMovement(@PathVariable Integer id, @RequestBody CreateInventoryMovementRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        InventoryMovementResponse movement = inventoryMovementService.create(id, request, idempotencyKey,
                authentication);
        return ResponseEntity.created(URI.create("/api/v1/inventory/" + id + "/movements/" + movement.id()))
            .body(movement);
    }

}
