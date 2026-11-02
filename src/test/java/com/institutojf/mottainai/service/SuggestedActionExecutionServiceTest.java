package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.ExecuteSuggestedActionRequest;
import com.institutojf.mottainai.dto.response.PurchaseOrderResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.Alert;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.SuggestedAction;
import com.institutojf.mottainai.model.enums.PriorityLevel;
import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;
import com.institutojf.mottainai.model.enums.SuggestedActionStatus;
import com.institutojf.mottainai.model.enums.SuggestedActionType;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.repository.SuggestedActionRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestedActionExecutionServiceTest {

    @Mock
    private SuggestedActionRepository suggestedActionRepository;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private PromotionItemService promotionItemService;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private TransferService transferService;

    @Mock
    private DonationService donationService;

    @Mock
    private DisposalService disposalService;

    @Mock
    private ReplenishmentService replenishmentService;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SuggestedActionExecutionService service;

    private SuggestedAction action;

    @BeforeEach
    void setUp() {
        RetailStore store = new RetailStore();
        store.setId(11);
        Alert alert = new Alert();
        alert.setId(22);
        alert.setStore(store);
        action = new SuggestedAction();
        action.setId(33);
        action.setAlert(alert);
        action.setActionType(SuggestedActionType.PURCHASE_ORDER);
        action.setPriority(PriorityLevel.MEDIUM);
        action.setStatus(SuggestedActionStatus.PENDING);
        action.setSourceRecommendationUuid(UUID.randomUUID());
        action.setGeneratedAt(LocalDateTime.now());
        action.setCreatedAt(LocalDateTime.now());
        action.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should execute target and mark suggestion as executed")
    void executesTargetThenMarksSuggestionAsExecuted() {
        ExecuteSuggestedActionRequest request = purchaseOrderRequest();
        Company company = new Company();
        company.setId(10);
        action.getAlert().getStore().setCompany(company);
        Employee employee = new Employee();
        employee.setStore(action.getAlert().getStore());
        AppUser actor = new AppUser();
        actor.setId(44);
        actor.setEmployee(employee);
        when(suggestedActionRepository.findByIdForUpdate(33)).thenReturn(Optional.of(action));
        when(outboxEventRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(purchaseOrderService.create(any(), anyString(), eq(authentication)))
            .thenReturn(new PurchaseOrderResponse(55, 11, 66, 77, LocalDateTime.now(), LocalDate.now().plusDays(2),
                    PurchaseOrderStatus.PENDING, null, new BigDecimal("25.00"), 1, List.of()));
        when(inventoryAccess.currentUser(authentication)).thenReturn(actor);
        var response = service.execute(33, request, "key-1", authentication);
        assertEquals(SuggestedActionStatus.EXECUTED, response.suggestedAction().status());
        assertEquals("purchase_order", response.entityType());
        assertEquals(55, response.entityId());
        verify(suggestedActionRepository).save(action);
        verify(auditLogRepository).record(eq("suggested_action"), eq("UPDATE"), eq("33"), eq(44), any(), any());
    }

    @Test
    @DisplayName("Should leave suggestion pending when target creation fails")
    void leavesSuggestionPendingWhenTargetCreationFails() {
        when(suggestedActionRepository.findByIdForUpdate(33)).thenReturn(Optional.of(action));
        when(outboxEventRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(purchaseOrderService.create(any(), anyString(), eq(authentication)))
            .thenThrow(new BusinessException("Supplier must be active"));
        assertThrows(BusinessException.class,
                () -> service.execute(33, purchaseOrderRequest(), "key-2", authentication));
        assertEquals(SuggestedActionStatus.PENDING, action.getStatus());
        verify(suggestedActionRepository, never()).save(any());
        verify(auditLogRepository, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should refuse second execution after concurrent winner changed status")
    void refusesSecondExecutionAfterConcurrentWinnerChangedStatus() {
        action.setStatus(SuggestedActionStatus.EXECUTED);
        when(suggestedActionRepository.findByIdForUpdate(33)).thenReturn(Optional.of(action));
        assertThrows(BusinessException.class,
                () -> service.execute(33, purchaseOrderRequest(), "key-3", authentication));
        verify(outboxEventRepository).lockIdempotencyKey("key-3");
        verify(purchaseOrderService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Should return original result when same execution key is retried")
    void returnsOriginalResultWhenSameExecutionKeyIsRetried() {
        action.setStatus(SuggestedActionStatus.EXECUTED);
        OutboxEventRepository.Event event = new OutboxEventRepository.Event("SUGGESTED_ACTION_EXECUTED",
                "suggested_action", "33", "{}");
        when(suggestedActionRepository.findByIdForUpdate(33)).thenReturn(Optional.of(action));
        when(outboxEventRepository.findByIdempotencyKey("key-retry")).thenReturn(Optional.of(event));
        when(outboxEventRepository.readSuggestedActionExecution(event, 33))
            .thenReturn(Optional.of(new OutboxEventRepository.SuggestedActionExecution("purchase_order", 55)));
        var response = service.execute(33, purchaseOrderRequest(), "key-retry", authentication);
        assertEquals(55, response.entityId());
        assertEquals("purchase_order", response.entityType());
        verify(purchaseOrderService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject fields from another action type before creating target")
    void rejectsFieldsFromAnotherActionTypeBeforeCreatingTarget() {
        ExecuteSuggestedActionRequest base = purchaseOrderRequest();
        ExecuteSuggestedActionRequest request = new ExecuteSuggestedActionRequest(base.supplierId(),
                base.expectedDeliveryDate(), 99, null, null, null, null, null, null, null, null, base.items());
        when(suggestedActionRepository.findByIdForUpdate(33)).thenReturn(Optional.of(action));
        when(outboxEventRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.execute(33, request, "key-4", authentication));
        verify(purchaseOrderService, never()).create(any(), any(), any());
        assertEquals(SuggestedActionStatus.PENDING, action.getStatus());
    }

    private ExecuteSuggestedActionRequest purchaseOrderRequest() {
        return new ExecuteSuggestedActionRequest(66, LocalDate.now().plusDays(2), null, null, null, null, null, null,
                null, null, null, List.of(new ExecuteSuggestedActionRequest.Item(88, null, new BigDecimal("5"), null,
                        new BigDecimal("5.00"), null, null, null, null)));
    }

}
