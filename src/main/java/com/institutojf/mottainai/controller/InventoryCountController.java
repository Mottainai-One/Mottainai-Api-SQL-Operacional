package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.InventoryCountControllerApi;
import com.institutojf.mottainai.dto.request.CreateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.request.CreateInventoryCountRequest;
import com.institutojf.mottainai.dto.request.UpdateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.response.InventoryCountItemResponse;
import com.institutojf.mottainai.dto.response.InventoryCountResponse;
import com.institutojf.mottainai.service.InventoryCountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory-counts")
@RequiredArgsConstructor
public class InventoryCountController implements InventoryCountControllerApi {

    private final InventoryCountService inventoryCountService;

    @Override
    @PostMapping
    public ResponseEntity<InventoryCountResponse> create(@Valid @RequestBody CreateInventoryCountRequest request, Authentication authentication) {
        InventoryCountResponse count = inventoryCountService.create(request, authentication);
        return ResponseEntity.created(URI.create("/api/v1/inventory-counts/" + count.id())).body(count);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<InventoryCountResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(inventoryCountService.findAll(authentication));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<InventoryCountResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(inventoryCountService.findById(id, authentication));
    }

    @Override
    @PostMapping("/{id}/items")
    public ResponseEntity<InventoryCountItemResponse> addItem(@PathVariable Long id, @Valid @RequestBody CreateInventoryCountItemRequest request, Authentication authentication) {
        InventoryCountItemResponse item = inventoryCountService.addItem(id, request, authentication);
        return ResponseEntity.created(URI.create("/api/v1/inventory-counts/" + id + "/items/" + item.id())).body(item);
    }

    @Override
    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<InventoryCountItemResponse> updateItem(@PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody UpdateInventoryCountItemRequest request, Authentication authentication) {
        return ResponseEntity.ok(inventoryCountService.updateItem(id, itemId, request, authentication));
    }

    @Override
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId, Authentication authentication) {
        inventoryCountService.deleteItem(id, itemId, authentication);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{id}/finish")
    public ResponseEntity<InventoryCountResponse> finish(@PathVariable Long id, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        return ResponseEntity.ok(inventoryCountService.finish(id, idempotencyKey, authentication));
    }

}
