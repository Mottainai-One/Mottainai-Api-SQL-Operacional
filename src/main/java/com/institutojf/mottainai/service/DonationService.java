package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.*;
import com.institutojf.mottainai.dto.response.DonationResponse;
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
public class DonationService {

    private final DonationRepository donationRepository;

    private final BatchRepository batchRepository;

    private final InventoryRepository inventoryRepository;

    private final InventoryMovementService inventoryMovementService;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public DonationResponse create(CreateDonationRequest request, String key, Authentication authentication) {
        validateKey(key);
        AppUser actor = inventoryAccess.currentUser(authentication);
        String requestHash = IdempotencyHash.of(actor.getEmployee().getStore().getId() + "|" + request);
        outboxEventRepository.lockIdempotencyKey(key);

        var previous = outboxEventRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "DONATION_CREATED", "donation", requestHash))
                throw new ConflictException("Idempotency-Key was already used for a different request");
            return findById(Integer.valueOf(previous.get().aggregateId()), authentication);

        }
        Donation donation = new Donation();
        donation.setStore(actor.getEmployee().getStore());
        donation.setEmployee(actor.getEmployee());
        donation.setInstitution(request.institution());
        donation.setObservation(request.observation());
        donation.setSuggestedActionId(request.suggestedActionId());
        donation.setDonationDate(LocalDateTime.now());

        Set<Integer> batches = new HashSet<>();
        for (CreateDonationRequest.Item item : request.items()) {
            if (!batches.add(item.batchId()))
                throw new BusinessException("A batch cannot be repeated");
            Batch batch = activeBatch(item.batchId());
            requireInventory(donation.getStore().getId(), batch.getId());
            DonationItem entity = new DonationItem();
            entity.setBatch(batch);
            entity.setDonatedQuantity(item.donatedQuantity());
            donation.addItem(entity);
        }
        Donation saved = donationRepository.save(donation);
        auditLogRepository.record("donation", "INSERT", saved.getId().toString(), actor.getId(), null,
                Map.of("institution", saved.getInstitution(), "status", saved.getStatus()));
        outboxEventRepository.publish("DONATION_CREATED", "donation", saved.getId().toString(),
                Map.of("donation_id", saved.getId(), "request_hash", requestHash),
                actor.getEmployee().getStore().getCompany().getId(), saved.getStore().getId(), key);
        return DonationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> findAll(Integer requestedStoreId, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        validateRange(from, to);
        Integer storeId = inventoryAccess.resolveStoreId(authentication, requestedStoreId);
        return donationRepository.findByStore_IdAndDonationDateBetweenOrderByDonationDateDesc(storeId, from, to)
            .stream()
            .map(DonationResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public DonationResponse findById(Integer id, Authentication authentication) {
        Donation donation = donationRepository.findOneById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
        inventoryAccess.checkStoreAccess(authentication, donation.getStore().getId());
        return DonationResponse.from(donation);
    }

    @Transactional
    public DonationResponse updateStatus(Integer id, UpdateDonationStatusRequest request, Authentication authentication) {
        Donation donation = donationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
        inventoryAccess.checkStoreAccess(authentication, donation.getStore().getId());
        AppUser actor = inventoryAccess.currentUser(authentication);
        if (donation.getStatus() != DonationStatus.REGISTERED
                || (request.status() != DonationStatus.COMPLETED && request.status() != DonationStatus.CANCELED))
            throw new BusinessException("Invalid donation status transition");
        DonationStatus old = donation.getStatus();
        if (request.status() == DonationStatus.COMPLETED) {
            donation.getItems().forEach(item -> {
                Inventory inventory = requireInventory(donation.getStore().getId(), item.getBatch().getId());
                inventoryMovementService.create(inventory.getId(), new CreateInventoryMovementRequest(MovementType.DONATION, item.getDonatedQuantity().negate(), "Donation " + donation.getId()), authentication);
            });
        }
        donation.setStatus(request.status());
        Donation saved = donationRepository.save(donation);
        auditLogRepository.record("donation", "UPDATE", id.toString(), actor.getId(), Map.of("status", old),
                Map.of("status", saved.getStatus()));
        return DonationResponse.from(saved);
    }

    private Batch activeBatch(Integer id) {
        return batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException("Batch must be active"));
    }

    private Inventory requireInventory(Integer storeId, Integer batchId) {
        return inventoryRepository.findByStore_IdAndBatch_IdAndInventoryType(storeId, batchId, InventoryType.NORMAL)
            .filter(i -> i.getDeletedAt() == null)
            .orElseThrow(() -> new BusinessException("Batch is not in this store inventory"));
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid donation date range of at most six months is required");
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

}
