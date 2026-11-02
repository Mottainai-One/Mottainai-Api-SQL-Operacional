package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateDisposalRequest;
import com.institutojf.mottainai.dto.request.CreateDonationRequest;
import com.institutojf.mottainai.dto.request.CreatePromotionItemRequest;
import com.institutojf.mottainai.dto.request.CreatePromotionRequest;
import com.institutojf.mottainai.dto.request.CreatePurchaseOrderItemRequest;
import com.institutojf.mottainai.dto.request.CreatePurchaseOrderRequest;
import com.institutojf.mottainai.dto.request.CreateTransferRequest;
import com.institutojf.mottainai.dto.request.ExecuteSuggestedActionRequest;
import com.institutojf.mottainai.dto.response.ExecuteSuggestedActionResponse;
import com.institutojf.mottainai.dto.response.PromotionResponse;
import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.SuggestedAction;
import com.institutojf.mottainai.model.enums.SuggestedActionStatus;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.repository.SuggestedActionRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SuggestedActionExecutionService {

    private final SuggestedActionRepository suggestedActionRepository;

    private final PurchaseOrderService purchaseOrderService;

    private final PromotionService promotionService;

    private final PromotionItemService promotionItemService;

    private final PromotionRepository promotionRepository;

    private final TransferService transferService;

    private final DonationService donationService;

    private final DisposalService disposalService;

    private final ReplenishmentService replenishmentService;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public ExecuteSuggestedActionResponse execute(Integer id, ExecuteSuggestedActionRequest request, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        SuggestedAction action = suggestedActionRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Suggested action not found"));
        inventoryAccess.checkStoreAccess(authentication, action.getAlert().getStore().getId());
        outboxEventRepository.lockIdempotencyKey(idempotencyKey);
        Optional<OutboxEventRepository.Event> previous = outboxEventRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            OutboxEventRepository.SuggestedActionExecution execution = outboxEventRepository
                .readSuggestedActionExecution(previous.get(), action.getId())
                .orElseThrow(() -> new ConflictException("Idempotency-Key was already used"));
            if (action.getStatus() != SuggestedActionStatus.EXECUTED) {
                throw new ConflictException("Suggested action execution is inconsistent");
            }
            return new ExecuteSuggestedActionResponse(SuggestedActionResponse.fromEntity(action),
                    execution.entityType(), execution.entityId());
        }
        if (action.getStatus() != SuggestedActionStatus.PENDING && action.getStatus() != SuggestedActionStatus.APPROVED) {
            throw new BusinessException("Only pending or approved suggested actions can be executed");
        }
        String targetIdempotencyKey = IdempotencyHash.of("suggested-action-target|" + action.getId() + "|" + idempotencyKey);
        CreatedEntity created = switch (action.getActionType()) {
            case PURCHASE_ORDER -> createPurchaseOrder(request, targetIdempotencyKey, authentication);
            case PROMOTION -> createPromotion(action, request, targetIdempotencyKey, authentication);
            case TRANSFER -> createTransfer(action, request, targetIdempotencyKey, authentication);
            case DONATION -> createDonation(action, request, targetIdempotencyKey, authentication);
            case DISPOSAL -> createDisposal(action, request, targetIdempotencyKey, authentication);
            case REORDER -> createReplenishment(action, request, targetIdempotencyKey, authentication);
        };
        AppUser actor = inventoryAccess.currentUser(authentication);
        SuggestedActionStatus previousStatus = action.getStatus();
        action.setStatus(SuggestedActionStatus.EXECUTED);
        action.setUpdatedAt(LocalDateTime.now());
        suggestedActionRepository.save(action);

        auditLogRepository.record("suggested_action", "UPDATE", action.getId().toString(), actor.getId(),
                Map.of("status", previousStatus),
                Map.of("status", SuggestedActionStatus.EXECUTED, "entity_type", created.type(), "entity_id", created.id()));

        outboxEventRepository.publish("SUGGESTED_ACTION_EXECUTED", "suggested_action", action.getId().toString(),
                Map.of("suggested_action_id", action.getId(), "entity_type", created.type(), "entity_id", created.id()),
                actor.getEmployee().getStore().getCompany().getId(), action.getAlert().getStore().getId(),
                idempotencyKey);
        return new ExecuteSuggestedActionResponse(SuggestedActionResponse.fromEntity(action), created.type(), created.id());
    }

    private CreatedEntity createPurchaseOrder(ExecuteSuggestedActionRequest request, String key, Authentication authentication) {
        require(request.supplierId(), "supplierId");
        rejectTopLevel(request, TopLevelField.SUPPLIER_ID, TopLevelField.EXPECTED_DELIVERY_DATE, TopLevelField.OBSERVATION);
        List<CreatePurchaseOrderItemRequest> items = request.items().stream().map(item -> {
            require(item.productId(), "items.productId");
            requirePositive(item.requestedQuantity(), "items.requestedQuantity");
            requirePositive(item.unitCost(), "items.unitCost");
            rejectItem(item, ItemField.PRODUCT_ID, ItemField.REQUESTED_QUANTITY, ItemField.UNIT_COST);
            return new CreatePurchaseOrderItemRequest(item.productId(), item.requestedQuantity(), item.unitCost());
        }).toList();
        Integer entityId = purchaseOrderService
            .create(new CreatePurchaseOrderRequest(request.supplierId(), request.expectedDeliveryDate(), request.observation(), items), key, authentication)
            .id();
        return new CreatedEntity("purchase_order", entityId);
    }

    private CreatedEntity createPromotion(SuggestedAction action, ExecuteSuggestedActionRequest request, String key, Authentication authentication) {
        requireText(request.name(), "name");
        requireText(request.promotionType(), "promotionType");
        require(request.startsAt(), "startsAt");
        require(request.endsAt(), "endsAt");
        rejectTopLevel(request, TopLevelField.NAME, TopLevelField.DESCRIPTION, TopLevelField.PROMOTION_TYPE, TopLevelField.STARTS_AT, TopLevelField.ENDS_AT);
        PromotionResponse response = promotionService.createPromotion(
                new CreatePromotionRequest(action.getAlert().getStore().getId(), request.name(), request.description(), request.promotionType(), request.startsAt(), request.endsAt(), false), authentication);
        Promotion promotion = promotionRepository.findById(response.id()).orElseThrow();
        promotion.setSuggestedAction(action);
        promotionRepository.save(promotion);
        for (ExecuteSuggestedActionRequest.Item item : request.items()) {
            require(item.productId(), "items.productId");
            requirePositive(item.originalPrice(), "items.originalPrice");
            requirePositive(item.promotionalPrice(), "items.promotionalPrice");
            rejectItem(item, ItemField.PRODUCT_ID, ItemField.ORIGINAL_PRICE, ItemField.PROMOTIONAL_PRICE, ItemField.QUANTITY_AVAILABLE);
            promotionItemService
                .createPromotionItem(response.id(), new CreatePromotionItemRequest(item.productId(), item.originalPrice(), item.promotionalPrice(), item.quantityAvailable()), authentication);
        }
        AppUser actor = inventoryAccess.currentUser(authentication);
        outboxEventRepository.publish("PROMOTION_CREATED", "promotion", response.id().toString(),
                Map.of("promotion_id", response.id(), "suggested_action_id", action.getId()), actor.getEmployee().getStore().getCompany().getId(), action.getAlert().getStore().getId(), key);
        return new CreatedEntity("promotion", response.id());
    }

    private CreatedEntity createTransfer(SuggestedAction action, ExecuteSuggestedActionRequest request, String key, Authentication authentication) {
        require(request.destinationStoreId(), "destinationStoreId");
        rejectTopLevel(request, TopLevelField.DESTINATION_STORE_ID, TopLevelField.OBSERVATION);
        List<CreateTransferRequest.Item> items = request.items()
            .stream()
            .map(item -> transferItem(item, CreateTransferRequest.Item::new))
            .toList();
        Integer entityId = transferService
            .create(new CreateTransferRequest(request.destinationStoreId(), action.getId(), request.observation(), items), key, authentication)
            .id();
        return new CreatedEntity("transfer", entityId);
    }

    private CreatedEntity createDonation(SuggestedAction action, ExecuteSuggestedActionRequest request, String key, Authentication authentication) {
        requireText(request.institution(), "institution");
        rejectTopLevel(request, TopLevelField.INSTITUTION, TopLevelField.OBSERVATION);
        List<CreateDonationRequest.Item> items = request.items()
            .stream()
            .map(item -> transferItem(item, CreateDonationRequest.Item::new))
            .toList();
        Integer entityId = donationService
            .create(new CreateDonationRequest(request.institution(), action.getId(), request.observation(), items), key, authentication)
            .id();
        return new CreatedEntity("donation", entityId);
    }

    private CreatedEntity createDisposal(SuggestedAction action, ExecuteSuggestedActionRequest request, String key, Authentication authentication) {
        requireText(request.reason(), "reason");
        rejectTopLevel(request, TopLevelField.REASON, TopLevelField.OBSERVATION);
        List<CreateDisposalRequest.Item> items = request.items()
            .stream()
            .map(item -> transferItem(item, CreateDisposalRequest.Item::new))
            .toList();
        Integer entityId = disposalService
            .create(new CreateDisposalRequest(request.reason(), action.getId(), request.observation(), items), key, authentication)
            .id();
        return new CreatedEntity("disposal", entityId);
    }

    private CreatedEntity createReplenishment(SuggestedAction action, ExecuteSuggestedActionRequest request, String key, Authentication authentication) {
        rejectTopLevel(request);
        List<ReplenishmentService.ExplicitItem> items = request.items().stream().map(item -> {
            require(item.productId(), "items.productId");
            requirePositive(item.quantity(), "items.quantity");
            require(item.priority(), "items.priority");
            rejectItem(item, ItemField.PRODUCT_ID, ItemField.QUANTITY, ItemField.PRIORITY);
            return new ReplenishmentService.ExplicitItem(item.productId(), item.quantity(), item.priority());
        }).toList();
        Integer entityId = replenishmentService
                .createFromSuggestedAction(items, action.getId(), key, authentication)
                .id();
        return new CreatedEntity("replenishment_pre_list", entityId);
    }

    private <T> T transferItem(ExecuteSuggestedActionRequest.Item item, ItemFactory<T> factory) {
        require(item.batchId(), "items.batchId");
        requirePositive(item.quantity(), "items.quantity");
        rejectItem(item, ItemField.BATCH_ID, ItemField.QUANTITY);
        return factory.create(item.batchId(), item.quantity());
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

    private void require(Object value, String field) {
        if (value == null) {
            throw new BusinessException(field + " is required for this action type");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " is required for this action type");
        }
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException(field + " must be greater than zero");
        }
    }

    private void rejectTopLevel(ExecuteSuggestedActionRequest request, TopLevelField... allowedFields) {
        Set<TopLevelField> allowed = Set.of(allowedFields);
        for (TopLevelField field : TopLevelField.values()) {
            if (!allowed.contains(field) && field.value(request) != null) {
                throw new BusinessException("Request contains a field that does not belong to this action type");
            }
        }
    }

    private void rejectItem(ExecuteSuggestedActionRequest.Item item, ItemField... allowedFields) {
        Set<ItemField> allowed = Set.of(allowedFields);
        for (ItemField field : ItemField.values()) {
            if (!allowed.contains(field) && field.value(item) != null) {
                throw new BusinessException("An item contains a field that does not belong to this action type");
            }
        }
    }

    private enum TopLevelField {
        SUPPLIER_ID,
        EXPECTED_DELIVERY_DATE,
        DESTINATION_STORE_ID,
        INSTITUTION,
        REASON,
        NAME,
        DESCRIPTION,
        PROMOTION_TYPE,
        STARTS_AT,
        ENDS_AT,
        OBSERVATION;

        Object value(ExecuteSuggestedActionRequest request) {
            return switch (this) {
                case SUPPLIER_ID -> request.supplierId();
                case EXPECTED_DELIVERY_DATE -> request.expectedDeliveryDate();
                case DESTINATION_STORE_ID -> request.destinationStoreId();
                case INSTITUTION -> request.institution();
                case REASON -> request.reason();
                case NAME -> request.name();
                case DESCRIPTION -> request.description();
                case PROMOTION_TYPE -> request.promotionType();
                case STARTS_AT -> request.startsAt();
                case ENDS_AT -> request.endsAt();
                case OBSERVATION -> request.observation();
            };
        }
    }

    private enum ItemField {
        PRODUCT_ID,
        BATCH_ID,
        REQUESTED_QUANTITY,
        QUANTITY,
        UNIT_COST,
        ORIGINAL_PRICE,
        PROMOTIONAL_PRICE,
        QUANTITY_AVAILABLE,
        PRIORITY;

        Object value(ExecuteSuggestedActionRequest.Item item) {
            return switch (this) {
                case PRODUCT_ID -> item.productId();
                case BATCH_ID -> item.batchId();
                case REQUESTED_QUANTITY -> item.requestedQuantity();
                case QUANTITY -> item.quantity();
                case UNIT_COST -> item.unitCost();
                case ORIGINAL_PRICE -> item.originalPrice();
                case PROMOTIONAL_PRICE -> item.promotionalPrice();
                case QUANTITY_AVAILABLE -> item.quantityAvailable();
                case PRIORITY -> item.priority();
            };
        }
    }

    private record CreatedEntity(String type, Integer id) {
    }

    @FunctionalInterface
    private interface ItemFactory<T> {

        T create(Integer batchId, BigDecimal quantity);

    }

}
