package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateReceivingItemRequest;
import com.institutojf.mottainai.dto.request.CreateReceivingRequest;
import com.institutojf.mottainai.dto.response.ReceivingItemResponse;
import com.institutojf.mottainai.dto.response.ReceivingResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;
import com.institutojf.mottainai.model.enums.ReceivingStatus;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.PurchaseOrderRepository;
import com.institutojf.mottainai.repository.ReceivingRepository;
import com.institutojf.mottainai.repository.ReceivingRepository.ReceivingData;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceivingService {

    private final ReceivingRepository receivingRepository;

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public ReceivingResponse create(CreateReceivingRequest request, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        AppUser actor = inventoryAccess.currentUser(authentication);
        String requestHash = requestHash(request);
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        var previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isReceivingFor(previous.get(), request.purchaseOrderId(), requestHash)) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            return getById(Integer.valueOf(previous.get().aggregateId()), authentication);
        }

        var order = purchaseOrderRepository.findByIdForUpdate(request.purchaseOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));

        inventoryAccess.checkStoreAccess(authentication, order.storeId());

        if (order.status() != PurchaseOrderStatus.APPROVED) {
            throw new BusinessException("Only approved purchase orders can be received");
        }

        if (receivingRepository.existsForPurchaseOrder(order.id(), order.orderDate())) {
            throw new ConflictException("Purchase order already has a receiving");
        }

        validateItems(order, request.items());
        ReceivingData created = receivingRepository.insert(order, actor.getEmployee().getId(), request.observation(), request.items());
        auditLogRepository.record("receiving", "INSERT", created.id().toString(), actor.getId(), null, created);
        outboxEventRepository.publishReceiving(created.id().toString(), order.id(), actor.getEmployee().getStore().getCompany().getId(), order.storeId(), requestHash, idempotencyKey);
        return toResponse(created);
    }

    @Transactional(readOnly = true)
    public List<ReceivingResponse> findAll(Integer requestedStoreId, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        validateRange(from, to);
        Integer storeId = inventoryAccess.resolveStoreId(authentication, requestedStoreId);
        return receivingRepository.findAll(storeId, from, to).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReceivingResponse getById(Integer id, Authentication authentication) {
        ReceivingData receiving = findAccessible(id, authentication, false);
        return toResponse(receiving);
    }

    @Transactional
    public ReceivingResponse updateStatus(Integer id, ReceivingStatus target, Authentication authentication) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        ReceivingData current = findAccessible(id, authentication, true);
        if (current.status() != ReceivingStatus.PENDING || target == ReceivingStatus.PENDING) {
            throw new BusinessException("Invalid receiving status transition");
        }

        boolean divergent = current.items()
            .stream()
            .anyMatch(item -> item.requestedQuantity().compareTo(item.receivedQuantity()) != 0);

        if (target == ReceivingStatus.CONFIRMED && divergent) {
            throw new BusinessException("A receiving with quantity differences cannot be confirmed");
        }
        if (target == ReceivingStatus.DIVERGENT && !divergent) {
            throw new BusinessException("A receiving without quantity differences cannot be marked divergent");
        }
        if (target == ReceivingStatus.CONFIRMED) {
            receivingRepository.creditInventory(id, current.storeId(), actor.getEmployee().getId());
        }

        receivingRepository.updateStatus(id, target);
        ReceivingData updated = receivingRepository.findById(id).orElseThrow();
        auditLogRepository.record("receiving", "UPDATE", id.toString(), actor.getId(), current, updated);
        return toResponse(updated);
    }

    private ReceivingData findAccessible(Integer id, Authentication authentication, boolean lock) {
        ReceivingData receiving = (lock ? receivingRepository.findByIdForUpdate(id) : receivingRepository.findById(id))
            .orElseThrow(() -> new ResourceNotFoundException("Receiving not found"));
        inventoryAccess.checkStoreAccess(authentication, receiving.storeId());
        return receiving;
    }

    private void validateItems(PurchaseOrderRepository.PurchaseOrderData order, List<CreateReceivingItemRequest> items) {
        Set<Integer> expected = order.items()
            .stream()
            .map(PurchaseOrderRepository.PurchaseOrderItemData::id)
            .collect(java.util.stream.Collectors.toSet());
        Set<Integer> received = new HashSet<>();
        for (CreateReceivingItemRequest item : items) {
            if (!received.add(item.purchaseOrderItemId())) {
                throw new BusinessException("Receiving items cannot be duplicated");
            }
            if (item.manufactureDate() != null && item.manufactureDate().isAfter(item.expirationDate())) {
                throw new BusinessException("Manufacture date cannot be after expiration date");
            }
        }
        if (!received.equals(expected)) {
            throw new BusinessException("Receiving must contain every purchase order item exactly once");
        }
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid receiving date range of at most six months is required");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

    private String requestHash(CreateReceivingRequest request) {
        String canonical = request.purchaseOrderId() + "|" + request.observation() + "|" + request.items();
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ReceivingResponse toResponse(ReceivingData receiving) {
        return new ReceivingResponse(receiving.id(), receiving.purchaseOrderId(), receiving.storeId(),
                receiving.employeeId(), receiving.receivingDate(), receiving.status(), receiving.observation(),
                receiving.items()
                    .stream()
                    .map(item -> new ReceivingItemResponse(item.id(), item.purchaseOrderItemId(), item.productId(),
                            item.requestedQuantity(), item.receivedQuantity(), item.unitCost(), item.manufactureDate(),
                            item.expirationDate(), item.observation()))
                    .toList());
    }

}
