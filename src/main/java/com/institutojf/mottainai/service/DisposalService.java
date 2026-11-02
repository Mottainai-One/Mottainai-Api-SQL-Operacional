package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.*;
import com.institutojf.mottainai.dto.response.DisposalResponse;
import com.institutojf.mottainai.exception.*;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.model.enums.*;
import com.institutojf.mottainai.repository.*;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DisposalService {

    private final DisposalRepository disposalRepository;

    private final BatchRepository batchRepository;

    private final InventoryRepository inventoryRepository;

    private final InventoryMovementService inventoryMovementService;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public DisposalResponse create(CreateDisposalRequest request, String key, Authentication authentication) {
        validateKey(key);
        AppUser actor = inventoryAccess.currentUser(authentication);
        String requestHash = IdempotencyHash.of(actor.getEmployee().getStore().getId() + "|" + request);
        outboxEventRepository.lockIdempotencyKey(key);

        var previous = outboxEventRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "DISPOSAL_CREATED", "disposal", requestHash))
                throw new ConflictException("Idempotency-Key was already used for a different request");
            return findById(Integer.valueOf(previous.get().aggregateId()), authentication);
        }

        Disposal disposal = new Disposal();
        disposal.setStore(actor.getEmployee().getStore());
        disposal.setEmployee(actor.getEmployee());
        disposal.setReason(request.reason());
        disposal.setObservation(request.observation());
        disposal.setSuggestedActionId(request.suggestedActionId());
        disposal.setDisposalDate(LocalDateTime.now());

        Set<Integer> batches = new HashSet<>();
        for (CreateDisposalRequest.Item item : request.items()) {
            if (!batches.add(item.batchId()))
                throw new BusinessException("A batch cannot be repeated");
            Batch batch = batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(item.batchId())
                .orElseThrow(() -> new BusinessException("Batch must be active"));
            Inventory inventory = requireInventory(disposal.getStore().getId(), batch.getId());
            DisposalItem entity = new DisposalItem();
            entity.setBatch(batch);
            entity.setDisposedQuantity(item.disposedQuantity());
            disposal.addItem(entity);
            inventoryMovementService.create(inventory.getId(), new CreateInventoryMovementRequest(MovementType.DISPOSAL, item.disposedQuantity().negate(), request.reason()), authentication);
        }
        Disposal saved = disposalRepository.save(disposal);
        auditLogRepository.record("disposal", "INSERT", saved.getId().toString(), actor.getId(), null,
                Map.of("reason", saved.getReason()));
        outboxEventRepository.publish("DISPOSAL_CREATED", "disposal", saved.getId().toString(),
                Map.of("disposal_id", saved.getId(), "request_hash", requestHash),
                actor.getEmployee().getStore().getCompany().getId(), saved.getStore().getId(), key);
        return DisposalResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DisposalResponse> findAll(Integer requestedStoreId, String reason, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        validateRange(from, to);
        Integer storeId = inventoryAccess.resolveStoreId(authentication, requestedStoreId);
        return disposalRepository
            .findByStore_IdAndDisposalDateBetweenAndReasonContainingIgnoreCaseOrderByDisposalDateDesc(storeId, from, to, reason == null ? "" : reason)
            .stream()
            .map(DisposalResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public DisposalResponse findById(Integer id, Authentication authentication) {
        Disposal disposal = disposalRepository.findOneById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Disposal not found"));
        inventoryAccess.checkStoreAccess(authentication, disposal.getStore().getId());
        return DisposalResponse.from(disposal);
    }

    private Inventory requireInventory(Integer storeId, Integer batchId) {
        return inventoryRepository.findByStore_IdAndBatch_IdAndInventoryType(storeId, batchId, InventoryType.NORMAL)
            .filter(i -> i.getDeletedAt() == null)
            .orElseThrow(() -> new BusinessException("Batch is not in this store inventory"));
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid disposal date range of at most six months is required");
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

}
