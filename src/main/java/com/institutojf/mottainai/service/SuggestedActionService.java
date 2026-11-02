package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSuggestedActionRequest;
import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.model.Alert;
import com.institutojf.mottainai.model.SuggestedAction;
import com.institutojf.mottainai.model.enums.SuggestedActionStatus;
import com.institutojf.mottainai.repository.AlertRepository;
import com.institutojf.mottainai.repository.SuggestedActionRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;

@Service
@RequiredArgsConstructor
public class SuggestedActionService {

    private final SuggestedActionRepository suggestedActionRepository;

    private final AlertRepository alertRepository;

    private final InventoryAccess inventoryAccess;

    private SuggestedActionResponse createSuggestedAction(Alert alert, CreateSuggestedActionRequest request) {
        SuggestedAction action = new SuggestedAction();
        action.setAlert(alert);
        action.setActionType(request.actionType());
        action.setDescription(request.description());
        action.setPriority(request.priority());
        action.setStatus(SuggestedActionStatus.PENDING);
        action.setSourceRecommendationUuid(request.sourceRecommendationUuid());
        action.setGeneratedAt(LocalDateTime.now());
        action.setCreatedAt(LocalDateTime.now());
        action.setUpdatedAt(LocalDateTime.now());
        return SuggestedActionResponse.fromEntity(suggestedActionRepository.save(action));
    }

    @Transactional(readOnly = true)
    public List<SuggestedActionResponse> getActionsByAlert(Integer alertId, Authentication authentication) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        inventoryAccess.checkStoreAccess(authentication, alert.getStore().getId());
        return suggestedActionRepository.findByAlert_IdOrderByGeneratedAtDesc(alertId)
            .stream()
            .map(SuggestedActionResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SuggestedActionResponse> getActionsByStore(Integer storeId, Authentication authentication) {
        inventoryAccess.checkStoreAccess(authentication, storeId);
        return suggestedActionRepository.findByAlert_Store_IdOrderByGeneratedAtDesc(storeId)
                .stream().map(SuggestedActionResponse::fromEntity).toList();
    }

    @Transactional
    public SuggestedActionResponse createForAlert(Integer alertId, CreateSuggestedActionRequest request, Authentication authentication) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        inventoryAccess.checkStoreAccess(authentication, alert.getStore().getId());
        var existing = suggestedActionRepository.findBySourceRecommendationUuid(request.sourceRecommendationUuid());
        if (existing.isPresent()) {
            SuggestedAction current = existing.get();
            if (!current.getAlert().getId().equals(alertId) || current.getActionType() != request.actionType()) {
                throw new ConflictException("Recommendation UUID was already used for different data");
            }
            return SuggestedActionResponse.fromEntity(current);
        }
        return createSuggestedAction(alert, request);
    }

    @Transactional
    public SuggestedActionResponse decide(Integer id, SuggestedActionStatus decision, Authentication authentication) {
        SuggestedAction action = suggestedActionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Suggested action not found"));
        inventoryAccess.checkStoreAccess(authentication, action.getAlert().getStore().getId());
        if (action.getStatus() != SuggestedActionStatus.PENDING) {
            throw new BusinessException("Only pending suggested actions can be decided");
        }
        if (decision != SuggestedActionStatus.APPROVED && decision != SuggestedActionStatus.REJECTED) {
            throw new BusinessException("Decision must be APPROVED or REJECTED");
        }
        action.setStatus(decision);
        action.setUpdatedAt(LocalDateTime.now());
        return SuggestedActionResponse.fromEntity(suggestedActionRepository.save(action));
    }

}
