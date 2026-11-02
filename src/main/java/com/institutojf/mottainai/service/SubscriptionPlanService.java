package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.request.UpdateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.SubscriptionPlanMapper;
import com.institutojf.mottainai.model.SubscriptionPlan;
import com.institutojf.mottainai.repository.CompanyRepository;
import com.institutojf.mottainai.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private final CompanyRepository companyRepository;

    private final SubscriptionPlanMapper subscriptionPlanMapper;

    @Transactional
    public SubscriptionPlanResponse create(CreateSubscriptionPlanRequest request) {
        if (subscriptionPlanRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Subscription plan name already exists");
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setActive(true);
        applyPlanFields(request.name(), request.description(), request.price(), request.storeLimit(),
                request.userLimit(), plan);
        return subscriptionPlanMapper.toResponse(subscriptionPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionPlanResponse> findAll(Pageable pageable) {
        return subscriptionPlanRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable)
            .map(subscriptionPlanMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse findById(Integer id) {
        return subscriptionPlanMapper.toResponse(findActivePlanById(id));
    }

    @Transactional
    public SubscriptionPlanResponse update(Integer id, UpdateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = findPlanById(id);
        if (subscriptionPlanRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Subscription plan name already exists");
        }
        if (Boolean.FALSE.equals(request.active())) {
            ensureCanDeactivate(id);
        }
        applyPlanFields(request.name(), request.description(), request.price(), request.storeLimit(),
                request.userLimit(), plan);
        plan.setActive(request.active());
        return subscriptionPlanMapper.toResponse(subscriptionPlanRepository.save(plan));
    }

    @Transactional
    public void delete(Integer id) {
        SubscriptionPlan plan = findActivePlanById(id);
        ensureCanDeactivate(id);
        plan.setActive(false);
        plan.setDeletedAt(LocalDateTime.now());
        subscriptionPlanRepository.save(plan);
    }

    private void ensureCanDeactivate(Integer id) {
        if (companyRepository.existsByPlan_IdAndActiveTrueAndDeletedAtIsNull(id)) {
            throw new BusinessException("Subscription plan has active companies");
        }
    }

    private SubscriptionPlan findActivePlanById(Integer id) {
        return subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));
    }

    private SubscriptionPlan findPlanById(Integer id) {
        return subscriptionPlanRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));
    }

    private void applyPlanFields(String name, String description, BigDecimal price, Integer storeLimit, Integer userLimit, SubscriptionPlan plan) {
        plan.setName(name);
        plan.setDescription(description);
        plan.setPrice(price);
        plan.setStoreLimit(storeLimit);
        plan.setUserLimit(userLimit);
    }

}
