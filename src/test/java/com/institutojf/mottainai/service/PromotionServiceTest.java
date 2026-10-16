package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreatePromotionRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.enums.PromotionStatus;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PromotionService service;

    @Test
    @DisplayName("Should reject update for promotion from another store")
    void shouldRejectUpdateForPromotionFromAnotherStore() {
        Promotion promotion = promotionInStore(10);
        when(promotionRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(promotion));
        org.mockito.Mockito.doThrow(new BusinessException("User cannot access this store"))
            .when(inventoryAccess)
            .checkStoreAccess(authentication, 10);
        assertThrows(BusinessException.class, () -> service.getPromotionById(7, authentication));
        verify(inventoryAccess).checkStoreAccess(authentication, 10);
    }

    @Test
    @DisplayName("Should create a manager promotion pending approval with its creator")
    void shouldCreatePendingPromotionForManager() {
        stubRole("MANAGER");
        RetailStore store = new RetailStore();
        store.setId(10);
        AppUser manager = userWithRole("MANAGER", 21, 31);
        when(inventoryAccess.currentUser(authentication)).thenReturn(manager);
        when(retailStoreRepository.findById(10)).thenReturn(Optional.of(store));
        when(promotionRepository.save(org.mockito.ArgumentMatchers.any(Promotion.class))).thenAnswer(invocation -> {
            Promotion promotion = invocation.getArgument(0);
            promotion.setId(7);
            return promotion;
        });
        service.createPromotion(
                new CreatePromotionRequest(10, "Queima de estoque", null, "SPECIAL_PRICE",
                        LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 2, 18, 0), true),
                authentication);
        org.mockito.ArgumentCaptor<Promotion> promotionCaptor = org.mockito.ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(promotionCaptor.capture());
        Promotion promotion = promotionCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(PromotionStatus.PENDING_APPROVAL, promotion.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(promotion.getActive());
        org.junit.jupiter.api.Assertions.assertSame(manager.getEmployee(), promotion.getCreatedBy());
        verify(auditLogRepository).record(org.mockito.ArgumentMatchers.eq("promotion"),
                org.mockito.ArgumentMatchers.eq("INSERT"), org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq(21), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should approve a pending promotion with the administrator as approver")
    void shouldApprovePendingPromotion() {
        stubRole("ADMINISTRATOR");
        Promotion promotion = promotionInStore(10);
        promotion.setId(7);
        promotion.setStatus(PromotionStatus.PENDING_APPROVAL);
        promotion.setActive(false);
        AppUser administrator = userWithRole("ADMINISTRATOR", 22, 32);
        when(promotionRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(promotion));
        when(inventoryAccess.currentUser(authentication)).thenReturn(administrator);
        when(promotionRepository.save(promotion)).thenReturn(promotion);
        service.approvePromotion(7, authentication);
        org.junit.jupiter.api.Assertions.assertEquals(PromotionStatus.APPROVED, promotion.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(promotion.getActive());
        org.junit.jupiter.api.Assertions.assertSame(administrator.getEmployee(), promotion.getApprovedBy());
        verify(auditLogRepository).record(org.mockito.ArgumentMatchers.eq("promotion"),
                org.mockito.ArgumentMatchers.eq("UPDATE"), org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq(22), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should reject approval of a promotion outside the pending state")
    void shouldRejectApprovalOutsidePendingState() {
        stubRole("ADMINISTRATOR");
        Promotion promotion = promotionInStore(10);
        promotion.setStatus(PromotionStatus.APPROVED);
        when(promotionRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(promotion));
        when(inventoryAccess.currentUser(authentication)).thenReturn(userWithRole("ADMINISTRATOR", 22, 32));
        assertThrows(BusinessException.class, () -> service.rejectPromotion(7, authentication));
        verify(promotionRepository, org.mockito.Mockito.never()).save(promotion);
    }

    @Test
    @DisplayName("Should cancel a non-terminal promotion")
    void shouldCancelPromotion() {
        stubRole("ADMINISTRATOR");
        Promotion promotion = promotionInStore(10);
        promotion.setId(7);
        promotion.setStatus(PromotionStatus.APPROVED);
        promotion.setActive(true);
        when(promotionRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(promotion));
        when(inventoryAccess.currentUser(authentication)).thenReturn(userWithRole("ADMINISTRATOR", 22, 32));
        when(promotionRepository.save(promotion)).thenReturn(promotion);
        service.cancelPromotion(7, authentication);
        org.junit.jupiter.api.Assertions.assertEquals(PromotionStatus.CANCELLED, promotion.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(promotion.getActive());
        verify(auditLogRepository).record(org.mockito.ArgumentMatchers.eq("promotion"),
                org.mockito.ArgumentMatchers.eq("UPDATE"), org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq(22), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should logically delete a manager promotion")
    void shouldLogicallyDeletePromotion() {
        stubRole("MANAGER");
        Promotion promotion = promotionInStore(10);
        promotion.setId(7);
        promotion.setStatus(PromotionStatus.PENDING_APPROVAL);
        when(promotionRepository.findByIdAndDeletedAtIsNull(7)).thenReturn(Optional.of(promotion));
        when(inventoryAccess.currentUser(authentication)).thenReturn(userWithRole("MANAGER", 21, 31));
        when(promotionRepository.save(promotion)).thenReturn(promotion);
        service.deletePromotion(7, authentication);
        org.junit.jupiter.api.Assertions.assertNotNull(promotion.getDeletedAt());
        org.junit.jupiter.api.Assertions.assertFalse(promotion.getActive());
        verify(auditLogRepository).record(org.mockito.ArgumentMatchers.eq("promotion"),
                org.mockito.ArgumentMatchers.eq("DELETE"), org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq(21), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private Promotion promotionInStore(Integer storeId) {
        RetailStore store = new RetailStore();
        store.setId(storeId);
        Promotion promotion = new Promotion();
        promotion.setStore(store);
        return promotion;
    }

    private AppUser userWithRole(String roleName, Integer userId, Integer employeeId) {
        EmployeeRole role = new EmployeeRole();
        role.setName(roleName);
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setRole(role);
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmployee(employee);
        return user;
    }

    private void stubRole(String roleName) {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_" + roleName))).when(authentication).getAuthorities();
    }

}
