package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateLoyaltyRewardRequest;
import com.institutojf.mottainai.dto.request.UpdateLoyaltyRewardRequest;
import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.dto.response.LoyaltyRewardResponse;
import com.institutojf.mottainai.dto.response.LoyaltyTransactionResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.LoyaltyAccount;
import com.institutojf.mottainai.model.LoyaltyReward;
import com.institutojf.mottainai.model.LoyaltyTransaction;
import com.institutojf.mottainai.repository.LoyaltyAccountRepository;
import com.institutojf.mottainai.repository.LoyaltyRewardRepository;
import com.institutojf.mottainai.repository.LoyaltyTransactionRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;

    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    private final LoyaltyRewardRepository loyaltyRewardRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public LoyaltyAccountResponse getLoyaltyAccount(Integer customerId) {
        LoyaltyAccount account = loyaltyAccountRepository.findByCustomer_Id(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
        return LoyaltyAccountResponse.fromEntity(account);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getTransactions(Integer customerId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid transaction date range of at most six months is required");
        }
        LoyaltyAccount account = loyaltyAccountRepository.findByCustomer_Id(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
        return loyaltyTransactionRepository
            .findByLoyaltyAccount_IdAndCreatedAtBetweenOrderByCreatedAtDesc(account.getId(), from, to)
            .stream()
            .map(LoyaltyTransactionResponse::fromEntity)
            .toList();
    }

    @Transactional
    public LoyaltyRewardResponse createReward(CreateLoyaltyRewardRequest request) {
        LoyaltyReward reward = new LoyaltyReward();
        reward.setName(request.name());
        reward.setDescription(request.description());
        reward.setPointsCost(request.pointsCost());
        reward.setActive(request.active() != null ? request.active() : true);
        reward.setValidFrom(request.validFrom());
        reward.setValidUntil(request.validUntil());
        reward.setCreatedAt(LocalDateTime.now());
        reward.setUpdatedAt(LocalDateTime.now());
        loyaltyRewardRepository.save(reward);
        auditLogRepository.record("loyalty_reward", "INSERT", reward.getId().toString(), null, null,
                Map.of("name", reward.getName(), "points_cost", reward.getPointsCost()));
        return LoyaltyRewardResponse.fromEntity(reward);
    }

    @Transactional
    public LoyaltyRewardResponse updateReward(Integer id, UpdateLoyaltyRewardRequest request) {
        LoyaltyReward reward = loyaltyRewardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reward not found"));
        Integer oldCost = reward.getPointsCost();
        reward.setPointsCost(request.pointsCost());
        reward.setUpdatedAt(LocalDateTime.now());
        loyaltyRewardRepository.save(reward);
        auditLogRepository.record("loyalty_reward", "UPDATE", id.toString(), null, Map.of("points_cost", oldCost),
                Map.of("points_cost", reward.getPointsCost()));
        return LoyaltyRewardResponse.fromEntity(reward);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyRewardResponse> getActiveRewards() {
        return loyaltyRewardRepository.findByActiveTrueOrderByName()
            .stream()
            .map(LoyaltyRewardResponse::fromEntity)
            .toList();
    }

}
