package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreatePurchaseOrderItemRequest;
import com.institutojf.mottainai.dto.request.CreatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.request.UpdatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.response.PurchaseOrderItemResponse;
import com.institutojf.mottainai.dto.response.PurchaseOrderResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.PurchaseOrderRepository;
import com.institutojf.mottainai.repository.PurchaseOrderRepository.PurchaseOrderData;
import com.institutojf.mottainai.repository.SupplierRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final SupplierRepository supplierRepository;

    private final ProductRepository productRepository;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        AppUser actor = requirePurchaseRole(authentication);
        Integer storeId = actor.getEmployee().getStore().getId();
        validateRequest(request.supplierId(), request.items());
        String requestHash = requestHash(storeId, request);
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        var previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);

        if (previous.isPresent()) {
            if (!outboxEventRepository.isPurchaseOrderFor(previous.get(), requestHash)) {
                throw new ConflictException("Idempotency-Key was already used for a different request");
            }
            return getById(Integer.valueOf(previous.get().aggregateId()), authentication);

        }
        PurchaseOrderData created = purchaseOrderRepository.insert(storeId, request.supplierId(), actor.getEmployee().getId(), request.expectedDeliveryDate(), request.observation(), request.items());
        auditLogRepository.record("purchase_order", "INSERT", created.id().toString(), actor.getId(), null, created);
        outboxEventRepository.publishPurchaseOrder(created.id().toString(), actor.getEmployee().getStore().getCompany().getId(), storeId, requestHash, idempotencyKey);
        return toResponse(created);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findAll(Integer requestedStoreId, LocalDateTime from, LocalDateTime to, Authentication authentication) {
        validateRange(from, to);
        Integer storeId = inventoryAccess.resolveStoreId(authentication, requestedStoreId);
        return purchaseOrderRepository.findAll(storeId, from, to).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Integer id, Authentication authentication) {
        PurchaseOrderData order = findAccessible(id, authentication, false);
        return toResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse update(Integer id, UpdatePurchaseOrderRequest request, Authentication authentication) {
        AppUser actor = requirePurchaseRole(authentication);
        PurchaseOrderData current = findAccessible(id, authentication, true);
        if (current.status() != PurchaseOrderStatus.PENDING) {
            throw new BusinessException("Only pending purchase orders can be updated");
        }
        Integer supplierId = request.supplierId() == null ? current.supplierId() : request.supplierId();
        validateRequest(supplierId, request.items());
        if (!purchaseOrderRepository.update(current, supplierId, request.expectedDeliveryDate(), request.observation(),
                request.items(), request.version())) {
            throw new ConflictException("Purchase order was modified by another transaction");
        }
        PurchaseOrderData updated = purchaseOrderRepository.findById(id).orElseThrow();
        auditLogRepository.record("purchase_order", "UPDATE", id.toString(), actor.getId(), current, updated);
        return toResponse(updated);
    }

    @Transactional
    public PurchaseOrderResponse updateStatus(Integer id, PurchaseOrderStatus target, Authentication authentication) {
        AppUser actor = requirePurchaseRole(authentication);
        PurchaseOrderData current = findAccessible(id, authentication, true);
        if (target == PurchaseOrderStatus.PENDING || current.status() != PurchaseOrderStatus.PENDING
                || target == current.status()) {
            throw new BusinessException("Invalid purchase order status transition");
        }
        if (!purchaseOrderRepository.updateStatus(current, target)) {
            throw new ConflictException("Purchase order was modified by another transaction");
        }
        PurchaseOrderData updated = purchaseOrderRepository.findById(id).orElseThrow();
        auditLogRepository.record("purchase_order", "UPDATE", id.toString(), actor.getId(), current, updated);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Integer id, Authentication authentication) {
        AppUser actor = requirePurchaseRole(authentication);
        PurchaseOrderData current = findAccessible(id, authentication, true);
        if (current.status() != PurchaseOrderStatus.PENDING) {
            throw new BusinessException("Only pending purchase orders can be deleted");
        }
        if (!purchaseOrderRepository.softDelete(current)) {
            throw new ConflictException("Purchase order was modified by another transaction");
        }
        auditLogRepository.record("purchase_order", "DELETE", id.toString(), actor.getId(), current, null);
    }

    private PurchaseOrderData findAccessible(Integer id, Authentication authentication, boolean lock) {
        PurchaseOrderData order = (lock ? purchaseOrderRepository.findByIdForUpdate(id)
                : purchaseOrderRepository.findById(id))
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
        inventoryAccess.checkStoreAccess(authentication, order.storeId());
        return order;
    }

    private AppUser requirePurchaseRole(Authentication authentication) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        boolean allowed = authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMINISTRATOR".equals(authority.getAuthority())
                        || "ROLE_MANAGER".equals(authority.getAuthority()));
        if (!allowed) {
            throw new BusinessException("Administrator or manager access is required");
        }
        return actor;
    }

    private void validateRequest(Integer supplierId, List<CreatePurchaseOrderItemRequest> items) {
        supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(supplierId)
            .orElseThrow(() -> new BusinessException("Supplier must be active"));
        for (CreatePurchaseOrderItemRequest item : items) {
            productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(item.productId())
                .orElseThrow(() -> new BusinessException("Every product must be active"));
        }
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid order date range of at most six months is required");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

    private String requestHash(Integer storeId, CreatePurchaseOrderRequest request) {
        String canonical = storeId + "|" + request.supplierId() + "|" + request.expectedDeliveryDate() + "|" + request.observation() + "|" + request.items();
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private PurchaseOrderResponse toResponse(PurchaseOrderData order) {
        return new PurchaseOrderResponse(
                order.id(),
                order.storeId(),
                order.supplierId(),
                order.employeeId(),
                order.orderDate(),
                order.expectedDeliveryDate(),
                order.status(),
                order.observation(),
                order.totalAmount(),
                order.version(),
                order.items()
                    .stream()
                    .map(item -> new PurchaseOrderItemResponse(item.id(), item.productId(), item.requestedQuantity(), item.unitCost(), item.subtotal()))
                    .toList());
    }

}
