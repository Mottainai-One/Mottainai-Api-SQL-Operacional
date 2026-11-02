package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateAlertRequest;
import com.institutojf.mottainai.dto.response.AlertResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Alert;
import com.institutojf.mottainai.model.enums.AlertStatus;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AlertRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    private final RetailStoreRepository retailStoreRepository;

    private final InventoryAccess inventoryAccess;

    @Transactional
    public AlertResponse createAlert(CreateAlertRequest request, Authentication authentication) {
        inventoryAccess.checkStoreAccess(authentication, request.storeId());
        RetailStore store = retailStoreRepository.findById(request.storeId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        Alert alert = new Alert();
        alert.setStore(store);
        alert.setTitle(request.title());
        alert.setDescription(request.description());
        alert.setAlertType(request.alertType());
        alert.setPriority(request.priority());
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setGeneratedAt(LocalDateTime.now());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        return AlertResponse.fromEntity(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByStore(Integer storeId, Authentication authentication) {
        inventoryAccess.checkStoreAccess(authentication, storeId);
        return alertRepository.findByStore_IdOrderByGeneratedAtDesc(storeId)
            .stream()
            .map(AlertResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Integer id, Authentication authentication) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        inventoryAccess.checkStoreAccess(authentication, alert.getStore().getId());
        return AlertResponse.fromEntity(alert);
    }

    @Transactional
    public AlertResponse updateStatus(Integer id, AlertStatus status, Authentication authentication) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        inventoryAccess.checkStoreAccess(authentication, alert.getStore().getId());
        alert.setStatus(status);
        alert.setResolvedAt(status == AlertStatus.RESOLVED ? LocalDateTime.now() : null);
        alert.setUpdatedAt(LocalDateTime.now());
        return AlertResponse.fromEntity(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public long countActiveAlerts(Integer storeId) {
        return alertRepository.countByStore_IdAndStatus(storeId, AlertStatus.ACTIVE);
    }

}
