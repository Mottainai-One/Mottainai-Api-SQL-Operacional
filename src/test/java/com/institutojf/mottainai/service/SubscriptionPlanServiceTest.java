package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.request.UpdateSubscriptionPlanRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.SubscriptionPlanMapper;
import com.institutojf.mottainai.model.SubscriptionPlan;
import com.institutojf.mottainai.repository.CompanyRepository;
import com.institutojf.mottainai.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SubscriptionPlanMapper subscriptionPlanMapper;

    @InjectMocks
    private SubscriptionPlanService subscriptionPlanService;

    @Test
    @DisplayName("Should create plan when name is available")
    void shouldCreatePlanWhenNameIsAvailable() {
        when(subscriptionPlanRepository.existsByNameIgnoreCase("Basic")).thenReturn(false);
        when(subscriptionPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        subscriptionPlanService
            .create(new CreateSubscriptionPlanRequest("Basic", "Entry plan", new BigDecimal("99.90"), 2, 5));
        verify(subscriptionPlanRepository).save(any());
    }

    @Test
    @DisplayName("Should reject plan when name already exists")
    void shouldRejectPlanWhenNameAlreadyExists() {
        when(subscriptionPlanRepository.existsByNameIgnoreCase("Basic")).thenReturn(true);
        assertThrows(ConflictException.class, () -> subscriptionPlanService
            .create(new CreateSubscriptionPlanRequest("Basic", "Entry plan", new BigDecimal("99.90"), 2, 5)));
        verify(subscriptionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject plan update when name belongs to another plan")
    void shouldRejectPlanUpdateWhenNameBelongsToAnotherPlan() {
        when(subscriptionPlanRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(plan(1, true)));
        when(subscriptionPlanRepository.existsByNameIgnoreCaseAndIdNot("Premium", 1)).thenReturn(true);
        assertThrows(ConflictException.class, () -> subscriptionPlanService.update(1,
                new UpdateSubscriptionPlanRequest("Premium", "Top plan", new BigDecimal("199.90"), 10, 50, true)));
        verify(subscriptionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update plan fields and reactivate it")
    void shouldUpdatePlanFieldsAndReactivateIt() {
        SubscriptionPlan plan = plan(1, false);
        when(subscriptionPlanRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(plan));
        when(subscriptionPlanRepository.existsByNameIgnoreCaseAndIdNot("Premium", 1)).thenReturn(false);
        when(subscriptionPlanRepository.save(plan)).thenReturn(plan);
        subscriptionPlanService.update(1,
                new UpdateSubscriptionPlanRequest("Premium", "Top plan", new BigDecimal("199.90"), 10, 50, true));
        assertEquals("Premium", plan.getName());
        assertEquals(10, plan.getStoreLimit());
        assertTrue(plan.getActive());
        verify(subscriptionPlanRepository).save(plan);
    }

    @Test
    @DisplayName("Should deactivate plan and mark it logically deleted")
    void shouldDeactivatePlanAndMarkItLogicallyDeleted() {
        SubscriptionPlan plan = plan(1, true);
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(plan));
        when(companyRepository.existsByPlan_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(false);
        subscriptionPlanService.delete(1);
        assertFalse(plan.getActive());
        assertNotNull(plan.getDeletedAt());
        verify(subscriptionPlanRepository).save(plan);
    }

    @Test
    @DisplayName("Should reject plan deactivation when it has active companies")
    void shouldRejectPlanDeactivationWhenItHasActiveCompanies() {
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1))
            .thenReturn(Optional.of(plan(1, true)));
        when(companyRepository.existsByPlan_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(true);
        assertThrows(BusinessException.class, () -> subscriptionPlanService.delete(1));
        verify(subscriptionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return not found when deactivating a nonexistent plan")
    void shouldReturnNotFoundWhenDeactivatingANonexistentPlan() {
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> subscriptionPlanService.delete(1));
        verify(subscriptionPlanRepository, never()).save(any());
    }

    private SubscriptionPlan plan(Integer id, boolean active) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(id);
        plan.setName("Basic");
        plan.setPrice(new BigDecimal("99.90"));
        plan.setStoreLimit(2);
        plan.setUserLimit(5);
        plan.setActive(active);
        return plan;
    }

}
