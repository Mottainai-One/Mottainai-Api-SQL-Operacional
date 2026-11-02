package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.request.CreateInventoryCountRequest;
import com.institutojf.mottainai.dto.request.UpdateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.response.InventoryCountItemResponse;
import com.institutojf.mottainai.dto.response.InventoryCountResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.InventoryCount;
import com.institutojf.mottainai.model.InventoryCountItem;
import com.institutojf.mottainai.model.enums.InventoryCountStatus;
import com.institutojf.mottainai.model.enums.MovementType;
import com.institutojf.mottainai.repository.InventoryCountItemRepository;
import com.institutojf.mottainai.repository.InventoryCountRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryCountService {

    private final InventoryCountRepository countRepository;

    private final InventoryCountItemRepository itemRepository;

    private final InventoryRepository inventoryRepository;

    private final InventoryAccess inventoryAccess;

    private final EntityManager entityManager;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public InventoryCountResponse create(CreateInventoryCountRequest request, Authentication authentication) {
        var user = inventoryAccess.currentUser(authentication);
        InventoryCount count = new InventoryCount();
        count.setStore(user.getEmployee().getStore());
        count.setEmployee(user.getEmployee());
        count.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        count.setStatus(InventoryCountStatus.IN_PROGRESS);
        count.setObservation(request.observation());
        return response(countRepository.save(count), List.of());
    }

    @Transactional(readOnly = true)
    public List<InventoryCountResponse> findAll(Authentication authentication) {
        Integer storeId = inventoryAccess.currentUser(authentication).getEmployee().getStore().getId();
        return countRepository.findAllByStore_IdOrderByStartedAtDesc(storeId)
            .stream()
            .map(count -> response(count, List.of()))
            .toList();
    }

    @Transactional(readOnly = true)
    public InventoryCountResponse findById(Long countId, Authentication authentication) {
        InventoryCount count = countRepository.findWithItemsById(countId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory count not found"));
        inventoryAccess.checkStoreAccess(authentication, count.getStore().getId());
        return response(count, count.getItems().stream().map(this::itemResponse).toList());
    }

    @Transactional
    public InventoryCountItemResponse addItem(Long countId, CreateInventoryCountItemRequest request, Authentication authentication) {
        InventoryCount count = editableCount(countId, authentication);
        Inventory inventory = inventoryRepository.findActiveByIdForUpdate(request.inventoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (!inventory.getStore().getId().equals(count.getStore().getId())) {
            throw new BusinessException("Inventory must belong to the count store");
        }

        InventoryCountItem item = itemRepository.findByInventoryCount_IdAndInventory_Id(countId, request.inventoryId())
            .map(existing -> {
                if (existing.getDeletedAt() == null) {
                    throw new BusinessException("Inventory item is already part of this count");
                }
                existing.setDeletedAt(null);
                return existing;
            })
            .orElseGet(InventoryCountItem::new);

        item.setInventoryCount(count);
        item.setInventory(inventory);
        item.setSystemQuantity(inventory.getCurrentQuantity());
        item.setCountedQuantity(request.countedQuantity());
        item.setObservation(request.observation());
        return itemResponse(itemRepository.save(item));
    }

    @Transactional
    public InventoryCountItemResponse updateItem(Long countId, Long itemId, UpdateInventoryCountItemRequest request, Authentication authentication) {
        editableCount(countId, authentication);

        InventoryCountItem item = itemRepository.findByIdAndCountIdForUpdate(countId, itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory count item not found"));

        item.setCountedQuantity(request.countedQuantity());
        item.setObservation(request.observation());
        return itemResponse(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long countId, Long itemId, Authentication authentication) {
        editableCount(countId, authentication);
        InventoryCountItem item = itemRepository.findByIdAndCountIdForUpdate(countId, itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory count item not found"));
        item.setDeletedAt(LocalDateTime.now());
        itemRepository.save(item);
    }

    @Transactional
    public InventoryCountResponse finish(Long countId, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        var previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "INVENTORY_COUNT_COMPLETED", "inventory_count")
                    || !countId.toString().equals(previous.get().aggregateId())) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            return findById(countId, authentication);
        }
        InventoryCount count = editableCount(countId, authentication);
        List<InventoryCountItem> items = itemRepository.findAllByCountIdWithInventory(countId);

        if (items.isEmpty()) {
            throw new BusinessException("An inventory count requires at least one item before completion");
        }

        Integer employeeId = inventoryAccess.currentUser(authentication).getEmployee().getId();
        for (InventoryCountItem item : items) {
            Inventory inventory = inventoryRepository.findActiveByIdForUpdate(item.getInventory().getId())
                .orElseThrow(() -> new BusinessException("Inventory is unavailable for count completion"));
            if (!inventory.getStore().getId().equals(count.getStore().getId())) {
                throw new BusinessException("Inventory must belong to the count store");
            }
            BigDecimal difference = item.getCountedQuantity().subtract(item.getSystemQuantity());
            if (difference.signum() != 0) {
                inventoryRepository.applyAtomicMovement(inventory.getId(), difference, MovementType.ADJUSTMENT.name(), employeeId,
                        "Inventory count " + countId, inventory.getVersion());
                entityManager.refresh(inventory);
            }
        }
        count.setStatus(InventoryCountStatus.COMPLETED);
        count.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        InventoryCount saved = countRepository.saveAndFlush(count);
        outboxEventRepository.publish("INVENTORY_COUNT_COMPLETED", "inventory_count", countId.toString(),
                Map.of("inventory_count_id", countId), count.getStore().getCompany().getId(), count.getStore().getId(),
                idempotencyKey);
        return response(saved, items.stream().map(this::itemResponse).toList());
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

    private InventoryCount editableCount(Long countId, Authentication authentication) {
        InventoryCount count = countRepository.findByIdForUpdate(countId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory count not found"));
        inventoryAccess.checkStoreAccess(authentication, count.getStore().getId());
        if (count.getStatus() != InventoryCountStatus.IN_PROGRESS) {
            throw new BusinessException("Only an inventory count in progress can be changed");
        }
        return count;
    }

    private InventoryCountResponse response(InventoryCount count, List<InventoryCountItemResponse> items) {
        return new InventoryCountResponse(
                count.getId(),
                count.getStore().getId(),
                count.getEmployee().getId(),
                count.getStatus(),
                count.getStartedAt(),
                count.getFinishedAt(),
                count.getObservation(),
                items
        );
    }

    private InventoryCountItemResponse itemResponse(InventoryCountItem item) {
        BigDecimal difference = item.getDifference() != null ? item.getDifference()
                : item.getCountedQuantity().subtract(item.getSystemQuantity());
        return new InventoryCountItemResponse(
                item.getId(),
                item.getInventory().getId(),
                item.getSystemQuantity(),
                item.getCountedQuantity(),
                difference,
                item.getObservation());
    }

}
