package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateInventoryMovementRequest;
import com.institutojf.mottainai.dto.request.CreateTransferRequest;
import com.institutojf.mottainai.dto.request.UpdateTransferStatusRequest;
import com.institutojf.mottainai.dto.response.TransferResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Batch;
import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.SuggestedAction;
import com.institutojf.mottainai.model.Transfer;
import com.institutojf.mottainai.model.TransferItem;
import com.institutojf.mottainai.model.enums.InventoryType;
import com.institutojf.mottainai.model.enums.MovementType;
import com.institutojf.mottainai.model.enums.SuggestedActionType;
import com.institutojf.mottainai.model.enums.TransferStatus;
import com.institutojf.mottainai.repository.BatchRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.repository.SuggestedActionRepository;
import com.institutojf.mottainai.repository.TransferRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;

    private final RetailStoreRepository retailStoreRepository;

    private final BatchRepository batchRepository;

    private final InventoryRepository inventoryRepository;

    private final SuggestedActionRepository suggestedActionRepository;

    private final InventoryMovementService inventoryMovementService;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public TransferResponse create(CreateTransferRequest request, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        validateItems(request.items());
        AppUser actor = inventoryAccess.currentUser(authentication);
        RetailStore source = actor.getEmployee().getStore();
        String requestHash = requestHash(source.getId(), actor.getEmployee().getId(), request);
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        var previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isTransferFor(previous.get(), requestHash)) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            return findById(Integer.valueOf(previous.get().aggregateId()), authentication);
        }
        RetailStore destination = retailStoreRepository
            .findByIdAndActiveTrueAndDeletedAtIsNull(request.destinationStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Destination store not found"));
        if (source.getId().equals(destination.getId())) {
            throw new BusinessException("Destination store must differ from source store");
        }
        Transfer transfer = new Transfer();
        transfer.setSourceStore(source);
        transfer.setDestinationStore(destination);
        transfer.setEmployee(actor.getEmployee());
        transfer.setSuggestedAction(resolveSuggestedAction(request.suggestedActionId(), source.getId()));
        transfer.setObservation(request.observation());
        transfer.setRequestDate(LocalDateTime.now());
        for (CreateTransferRequest.Item requestedItem : request.items()) {
            Batch batch = batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(requestedItem.batchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
            transfer.addItem(item(batch, requestedItem.transferredQuantity()));
        }
        Transfer saved = transferRepository.saveAndFlush(transfer);
        TransferResponse response = TransferResponse.from(saved);
        outboxEventRepository.publishTransfer(saved.getId().toString(), source.getCompany().getId(), source.getId(),
                requestHash, idempotencyKey);
        return response;
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> findAll(Integer storeId, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid transfer date range of at most six months is required");
        }
        Integer resolvedStore = inventoryAccess.resolveStoreId(authentication, storeId);
        return transferRepository.findHistory(resolvedStore, from, to).stream().map(TransferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse findById(Integer id, Authentication authentication) {
        return TransferResponse.from(accessible(id, authentication, false));
    }

    @Transactional
    public TransferResponse updateStatus(Integer id, UpdateTransferStatusRequest request, Authentication authentication) {
        Transfer transfer = accessible(id, authentication, true);
        if (request.status() == TransferStatus.IN_TRANSIT && transfer.getStatus() == TransferStatus.REQUESTED) {
            requireSource(transfer, authentication);
            for (TransferItem item : transfer.getItems()) {
                debit(transfer, item, authentication);
            }
        }
        else if (request.status() == TransferStatus.CANCELED && transfer.getStatus() == TransferStatus.IN_TRANSIT) {
            requireSource(transfer, authentication);
            for (TransferItem item : transfer.getItems()) {
                credit(transfer.getSourceStore().getId(), item, transfer, authentication, "Transfer cancellation ");
            }
        }
        else {
            throw new BusinessException("Invalid transfer status transition");
        }
        transfer.setStatus(request.status());
        Transfer saved = transferRepository.saveAndFlush(transfer);
        TransferResponse response = TransferResponse.from(saved);
        return response;
    }

    @Transactional
    public TransferResponse receive(Integer id, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        var previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "TRANSFER_RECEIVED", "transfer")
                    || !id.toString().equals(previous.get().aggregateId())) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            return findById(id, authentication);
        }
        Transfer transfer = accessible(id, authentication, true);
        Integer destinationId = transfer.getDestinationStore().getId();
        Integer storeId = inventoryAccess.resolveStoreId(authentication, destinationId);
        if (!storeId.equals(destinationId) || transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BusinessException("Transfer cannot be received");
        }
        retailStoreRepository.findByIdAndActiveTrueAndDeletedAtIsNull(destinationId)
            .orElseThrow(() -> new BusinessException("Destination store is inactive"));
        for (TransferItem item : transfer.getItems()) {
            credit(storeId, item, transfer, authentication, "Transfer receipt ");
        }
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletionDate(LocalDateTime.now());
        Transfer saved = transferRepository.saveAndFlush(transfer);
        TransferResponse response = TransferResponse.from(saved);
        outboxEventRepository.publish("TRANSFER_RECEIVED", "transfer", saved.getId().toString(),
                Map.of("transfer_id", saved.getId()), saved.getSourceStore().getCompany().getId(), destinationId,
                idempotencyKey);
        return response;
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

    private void validateItems(List<CreateTransferRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Transfer must contain at least one item");
        }
        Set<Integer> batchIds = new HashSet<>();
        for (CreateTransferRequest.Item item : items) {
            if (!batchIds.add(item.batchId())) {
                throw new BusinessException("Transfer contains a duplicate batch");
            }
        }
    }

    private SuggestedAction resolveSuggestedAction(Integer suggestedActionId, Integer sourceStoreId) {
        if (suggestedActionId == null) {
            return null;
        }
        SuggestedAction action = suggestedActionRepository.findById(suggestedActionId)
            .orElseThrow(() -> new ResourceNotFoundException("Suggested action not found"));
        if (action.getActionType() != SuggestedActionType.TRANSFER
                || !sourceStoreId.equals(action.getAlert().getStore().getId())) {
            throw new BusinessException("Suggested action does not belong to this transfer");
        }
        return action;
    }

    private TransferItem item(Batch batch, BigDecimal quantity) {
        TransferItem item = new TransferItem();
        item.setBatch(batch);
        item.setTransferredQuantity(quantity);
        return item;
    }

    private Transfer accessible(Integer id, Authentication authentication, boolean forUpdate) {
        Transfer transfer = (forUpdate ? transferRepository.findByIdForUpdate(id) : transferRepository.findById(id))
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
        Integer resolvedStore = inventoryAccess.resolveStoreId(authentication, transfer.getSourceStore().getId());
        if (!resolvedStore.equals(transfer.getSourceStore().getId())
                && !resolvedStore.equals(transfer.getDestinationStore().getId())) {
            throw new BusinessException("User cannot access this transfer");
        }
        return transfer;
    }

    private void requireSource(Transfer transfer, Authentication authentication) {
        inventoryAccess.checkStoreAccess(authentication, transfer.getSourceStore().getId());
    }

    private void debit(Transfer transfer, TransferItem item, Authentication authentication) {
        Batch batch = item.getBatch();
        if (!Boolean.TRUE.equals(batch.getActive()) || batch.getDeletedAt() != null) {
            throw new BusinessException("Transfer batch is inactive");
        }
        Inventory inventory = inventoryRepository
            .findByStore_IdAndBatch_IdAndInventoryTypeAndDeletedAtIsNull(transfer.getSourceStore().getId(),
                    batch.getId(), InventoryType.NORMAL)
            .orElseThrow(() -> new BusinessException("Active source inventory not found"));
        inventoryMovementService.create(inventory.getId(), new CreateInventoryMovementRequest(MovementType.TRANSFER,
                item.getTransferredQuantity().negate(), "Transfer " + transfer.getId()), authentication);
    }

    private void credit(Integer storeId, TransferItem item, Transfer transfer, Authentication authentication, String note) {
        Inventory inventory = inventoryRepository
            .findByStore_IdAndBatch_IdAndInventoryType(storeId, item.getBatch().getId(), InventoryType.NORMAL)
            .map(this::reactivate)
            .orElseGet(() -> createInventory(storeId, item.getBatch()));
        inventoryMovementService.create(inventory.getId(), new CreateInventoryMovementRequest(MovementType.TRANSFER,
                item.getTransferredQuantity(), note + transfer.getId()), authentication);
    }

    private Inventory reactivate(Inventory inventory) {
        if (inventory.getDeletedAt() != null) {
            inventory.setDeletedAt(null);
            return inventoryRepository.saveAndFlush(inventory);
        }
        return inventory;
    }

    private Inventory createInventory(Integer storeId, Batch batch) {
        Inventory inventory = new Inventory();
        inventory.setStore(retailStoreRepository.getReferenceById(storeId));
        inventory.setBatch(batch);
        inventory.setInventoryType(InventoryType.NORMAL);
        inventory.setCurrentQuantity(BigDecimal.ZERO);
        inventory.setMinimumQuantity(BigDecimal.ZERO);
        return inventoryRepository.saveAndFlush(inventory);
    }

    private String requestHash(Integer sourceStoreId, Integer employeeId, CreateTransferRequest request) {
        String items = request.items()
            .stream()
            .sorted(Comparator.comparing(CreateTransferRequest.Item::batchId))
            .map(item -> item.batchId() + ":" + item.transferredQuantity().stripTrailingZeros().toPlainString())
            .reduce((left, right) -> left + "," + right)
            .orElse("");
        String canonical = sourceStoreId + "|" + employeeId + "|" + request.destinationStoreId() + "|"
                + request.suggestedActionId() + "|" + request.observation() + "|" + items;
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

}
