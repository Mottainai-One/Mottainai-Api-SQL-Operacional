package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateReplenishmentExecutionRequest;
import com.institutojf.mottainai.dto.response.*;
import com.institutojf.mottainai.exception.*;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.model.enums.*;
import com.institutojf.mottainai.repository.*;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReplenishmentService {

    private final ReplenishmentPreListRepository preListRepository;

    private final ReplenishmentExecutionRepository executionRepository;

    private final InventoryRepository inventoryRepository;

    private final BatchRepository batchRepository;

    private final ProductRepository productRepository;

    private final InventoryAccess inventoryAccess;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public ReplenishmentPreListResponse generate(String key, Authentication authentication) {
        validateKey(key);
        AppUser actor = inventoryAccess.currentUser(authentication);
        String requestHash = IdempotencyHash.of("generate|" + actor.getEmployee().getStore().getId());
        outboxEventRepository.lockIdempotencyKey(key);
        var previous = outboxEventRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "REPLENISHMENT_PRE_LIST_CREATED", "replenishment_pre_list", requestHash))
                throw new ConflictException("Idempotency-Key was already used for a different request");
            return findPreList(Integer.valueOf(previous.get().aggregateId()), authentication);
        }
        ReplenishmentPreList list = new ReplenishmentPreList();
        list.setStore(actor.getEmployee().getStore());
        list.setEmployee(actor.getEmployee());
        list.setGeneratedAt(LocalDateTime.now());
        for (InventoryRepository.ReplenishmentShortage shortage : inventoryRepository.findReplenishmentShortages(list.getStore().getId())) {
            ReplenishmentPreListItem item = new ReplenishmentPreListItem();
            item.setProduct(shortage.getProduct());
            item.setSuggestedQuantity(shortage.getShortage());
            item.setPriority(priority(shortage));
            list.addItem(item);
        }
        ReplenishmentPreList saved = preListRepository.save(list);
        auditLogRepository.record("replenishment_pre_list", "INSERT", saved.getId().toString(), actor.getId(), null, Map.of("item_count", saved.getItems().size()));
        outboxEventRepository.publish("REPLENISHMENT_PRE_LIST_CREATED", "replenishment_pre_list", saved.getId().toString(), Map.of("pre_list_id", saved.getId(), "request_hash", requestHash), actor.getEmployee().getStore().getCompany().getId(), saved.getStore().getId(), key);
        return ReplenishmentPreListResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReplenishmentPreListResponse> findPending(Integer requestedStoreId, Authentication auth) {
        Integer storeId = inventoryAccess.resolveStoreId(auth, requestedStoreId);
        return preListRepository
            .findByStore_IdAndStatusInOrderByGeneratedAtDesc(storeId, List.of(PreListStatus.GENERATED, PreListStatus.IN_PROGRESS))
            .stream()
            .map(ReplenishmentPreListResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReplenishmentPreListResponse findPreList(Integer id, Authentication auth) {
        ReplenishmentPreList list = preListRepository.findOneById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Replenishment pre-list not found"));
        inventoryAccess.checkStoreAccess(auth, list.getStore().getId());
        return ReplenishmentPreListResponse.from(list);
    }

    @Transactional
    public ReplenishmentExecutionResponse execute(CreateReplenishmentExecutionRequest request, String key, Authentication auth) {
        validateKey(key);
        AppUser actor = inventoryAccess.currentUser(auth);
        String requestHash = IdempotencyHash.of(actor.getEmployee().getStore().getId() + "|" + request);
        outboxEventRepository.lockIdempotencyKey(key);
        var previous = outboxEventRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            if (!outboxEventRepository.isEventFor(previous.get(), "REPLENISHMENT_EXECUTED", "replenishment_execution", requestHash))
                throw new ConflictException("Idempotency-Key was already used for a different request");
            return findExecution(Integer.valueOf(previous.get().aggregateId()), auth);
        }
        ReplenishmentPreList list = preListRepository.findByIdForUpdate(request.preListId())
            .orElseThrow(() -> new ResourceNotFoundException("Replenishment pre-list not found"));
        inventoryAccess.checkStoreAccess(auth, list.getStore().getId());
        if (list.getStatus() != PreListStatus.GENERATED && list.getStatus() != PreListStatus.IN_PROGRESS)
            throw new BusinessException("Replenishment pre-list is closed");
        Set<Integer> products = new HashSet<>(list.getItems().stream().map(i -> i.getProduct().getId()).toList());
        Set<Integer> batches = new HashSet<>();
        ReplenishmentExecution execution = new ReplenishmentExecution();
        execution.setPreList(list);
        execution.setEmployee(actor.getEmployee());
        execution.setStartDate(LocalDateTime.now());
        execution.setEndDate(LocalDateTime.now());
        execution.setRating(request.rating());
        execution.setComment(request.comment());
        for (CreateReplenishmentExecutionRequest.Item requested : request.items()) {
            if (!batches.add(requested.batchId()))
                throw new BusinessException("A batch cannot be repeated");

            Batch batch = batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(requested.batchId())
                .orElseThrow(() -> new BusinessException("Batch must be active"));

            if (!products.contains(batch.getProduct().getId()))
                throw new BusinessException("Batch product is not in the pre-list");

            inventoryRepository
                .findByStore_IdAndBatch_IdAndInventoryTypeAndDeletedAtIsNull(list.getStore().getId(), batch.getId(), InventoryType.NORMAL)
                .orElseThrow(() -> new BusinessException("Batch is not in this store inventory"));

            ReplenishmentExecutionItem item = new ReplenishmentExecutionItem();
            item.setBatch(batch);
            item.setReplenishedQuantity(requested.replenishedQuantity());
            execution.addItem(item);
        }
        list.setStatus(PreListStatus.COMPLETED);
        ReplenishmentExecution saved = executionRepository.save(execution);
        auditLogRepository.record("replenishment_execution", "INSERT", saved.getId().toString(), actor.getId(), null, Map.of("pre_list_id", list.getId()));
        outboxEventRepository.publish("REPLENISHMENT_EXECUTED", "replenishment_execution", saved.getId().toString(), Map.of("execution_id", saved.getId(), "pre_list_id", list.getId(), "request_hash", requestHash), actor.getEmployee().getStore().getCompany().getId(), list.getStore().getId(), key);
        return ReplenishmentExecutionResponse.from(saved);
    }

    @Transactional
    public ReplenishmentPreListResponse createFromSuggestedAction(List<ExplicitItem> items, Integer suggestedActionId, String idempotencyKey, Authentication auth) {
        if (items == null || items.isEmpty())
            throw new BusinessException("At least one replenishment item is required");
        AppUser actor = inventoryAccess.currentUser(auth);
        ReplenishmentPreList list = new ReplenishmentPreList();
        list.setStore(actor.getEmployee().getStore());
        list.setEmployee(actor.getEmployee());
        list.setGeneratedAt(LocalDateTime.now());
        Set<Integer> products = new HashSet<>();
        for (ExplicitItem requested : items) {
            if (requested.productId() == null || requested.suggestedQuantity() == null
                    || requested.suggestedQuantity().signum() <= 0 || requested.priority() == null)
                throw new BusinessException("Replenishment item is invalid");
            if (!products.add(requested.productId()))
                throw new BusinessException("A product cannot be repeated");
            Product product = productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(requested.productId())
                .orElseThrow(() -> new BusinessException("Product must be active"));
            ReplenishmentPreListItem item = new ReplenishmentPreListItem();
            item.setProduct(product);
            item.setSuggestedQuantity(requested.suggestedQuantity());
            item.setPriority(requested.priority());
            list.addItem(item);
        }
        ReplenishmentPreList saved = preListRepository.save(list);
        auditLogRepository.record("replenishment_pre_list", "INSERT", saved.getId().toString(), actor.getId(), null,
                Map.of("item_count", saved.getItems().size(), "source", "suggested_action"));
        outboxEventRepository.publish("REPLENISHMENT_PRE_LIST_CREATED", "replenishment_pre_list", saved.getId().toString(), Map.of("pre_list_id", saved.getId(), "suggested_action_id", suggestedActionId), actor.getEmployee().getStore().getCompany().getId(), saved.getStore().getId(), idempotencyKey);
        return ReplenishmentPreListResponse.from(saved);
    }

    public record ExplicitItem(
            Integer productId,
            BigDecimal suggestedQuantity,
            PriorityLevel priority
    ) {
    }

    @Transactional(readOnly = true)
    public List<ReplenishmentExecutionResponse> findExecutions(Integer requestedStoreId, LocalDateTime from, LocalDateTime to, Authentication auth) {
        if (from == null && to == null) {
            from = LocalDate.now().atStartOfDay();
            to = from.plusDays(1).minusNanos(1);
        }
        validateRange(from, to);
        Integer storeId = inventoryAccess.resolveStoreId(auth, requestedStoreId);
        return executionRepository.findByPreList_Store_IdAndStartDateBetweenOrderByStartDateDesc(storeId, from, to)
            .stream()
            .map(ReplenishmentExecutionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReplenishmentExecutionResponse findExecution(Integer id, Authentication auth) {
        ReplenishmentExecution execution = executionRepository.findOneById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Replenishment execution not found"));
        inventoryAccess.checkStoreAccess(auth, execution.getPreList().getStore().getId());
        return ReplenishmentExecutionResponse.from(execution);
    }

    private PriorityLevel priority(InventoryRepository.ReplenishmentShortage shortage) {
        if (shortage.getMinimumQuantity().signum() == 0)
            return PriorityLevel.LOW;
        BigDecimal ratio = shortage.getCurrentQuantity().divide(shortage.getMinimumQuantity(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.25")) <= 0)
            return PriorityLevel.CRITICAL;
        if (ratio.compareTo(new BigDecimal("0.50")) <= 0)
            return PriorityLevel.HIGH;
        return PriorityLevel.MEDIUM;
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid execution date range of at most six months is required");
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 180) {
            throw new BusinessException("Idempotency-Key must contain between 1 and 180 characters");
        }
    }

}
