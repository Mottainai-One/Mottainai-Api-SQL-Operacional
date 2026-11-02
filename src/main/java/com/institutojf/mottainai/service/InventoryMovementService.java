package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateInventoryMovementRequest;
import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.InventoryMovementMapper;
import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.InventoryMovement;
import com.institutojf.mottainai.model.enums.MovementType;
import com.institutojf.mottainai.repository.InventoryMovementRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryMovementService {

    private final InventoryRepository inventoryRepository;

    private final InventoryMovementRepository inventoryMovementRepository;

    private final InventoryMovementMapper inventoryMovementMapper;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public InventoryMovementResponse create(Integer inventoryId, CreateInventoryMovementRequest request, Authentication authentication) {
        return createMovement(inventoryId, request, null, authentication);
    }

    @Transactional
    public InventoryMovementResponse create(Integer inventoryId, CreateInventoryMovementRequest request, String idempotencyKey, Authentication authentication) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
        String requestHash = IdempotencyHash.of(inventoryId + "|" + request.movementType() + "|"
                + request.movedQuantity() + "|" + request.observation());
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        var previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "INVENTORY_MOVEMENT_CREATED", "inventory_movement",
                    requestHash)) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            InventoryMovement movement = inventoryMovementRepository
                .findById(Integer.valueOf(previous.get().aggregateId()))
                .orElseThrow(() -> new ConflictException("Previous movement no longer exists"));
            return inventoryMovementMapper.toResponse(movement);
        }
        return createMovement(inventoryId, request, idempotencyKey, authentication);
    }

    private InventoryMovementResponse createMovement(Integer inventoryId, CreateInventoryMovementRequest request, String idempotencyKey, Authentication authentication) {
        Inventory inventory = inventoryRepository.findActiveByIdForUpdate(inventoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        inventoryAccess.checkStoreAccess(authentication, inventory.getStore().getId());
        validateDirection(request.movementType(), request.movedQuantity());
        BigDecimal previousBalance = inventory.getCurrentQuantity();
        BigDecimal currentBalance = previousBalance.add(request.movedQuantity());
        if (currentBalance.signum() < 0) {
            throw new BusinessException("Insufficient inventory balance");
        }
        inventory.setCurrentQuantity(currentBalance);
        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(inventory);
        movement.setEmployee(inventoryAccess.currentUser(authentication).getEmployee());
        movement.setMovementDate(LocalDateTime.now());
        movement.setMovementType(request.movementType());
        movement.setMovedQuantity(request.movedQuantity());
        movement.setPreviousBalance(previousBalance);
        movement.setCurrentBalance(currentBalance);
        movement.setObservation(request.observation());
        movement.setStoreId(inventory.getStore().getId());
        inventoryRepository.save(inventory);
        InventoryMovement saved = inventoryMovementRepository.save(movement);
        String requestHash = IdempotencyHash.of(inventoryId + "|" + request.movementType() + "|"
                + request.movedQuantity() + "|" + request.observation());
        outboxEventRepository.publish("INVENTORY_MOVEMENT_CREATED", "inventory_movement",
                saved.getMovementId().toString(),
                Map.of("movement_id", saved.getMovementId(), "inventory_id", inventoryId, "request_hash", requestHash),
                inventory.getStore().getCompany().getId(), inventory.getStore().getId(), idempotencyKey);
        return inventoryMovementMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> findByInventory(Integer inventoryId, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        validateDateRange(from, to);
        Inventory inventory = inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        inventoryAccess.checkStoreAccess(authentication, inventory.getStore().getId());
        return inventoryMovementRepository
            .findAllByInventory_IdAndMovementDateBetweenOrderByMovementDateDesc(inventoryId, from, to)
            .stream()
            .map(inventoryMovementMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> findByStore(Integer requestedStoreId, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        validateDateRange(from, to);
        Integer storeId = inventoryAccess.resolveStoreId(authentication, requestedStoreId);
        return inventoryMovementRepository
            .findAllByStoreIdAndMovementDateBetweenOrderByMovementDateDesc(storeId, from, to)
            .stream()
            .map(inventoryMovementMapper::toResponse)
            .toList();
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid movement date range of at most six months is required");
        }
    }

    private void validateDirection(MovementType type, BigDecimal quantity) {
        if (quantity.signum() == 0) {
            throw new BusinessException("Movement quantity cannot be zero");
        }
        switch (type) {
            case IN:
                if (quantity.signum() < 0) {
                    throw new BusinessException("IN movements must have positive quantity");
                }
                break;
            case OUT:
            case DISPOSAL:
                if (quantity.signum() > 0) {
                    throw new BusinessException("OUT and DISPOSAL movements must have negative quantity");
                }
                break;
            case ADJUSTMENT:
            case TRANSFER:
            case DONATION:
                break;
        }
    }

}
