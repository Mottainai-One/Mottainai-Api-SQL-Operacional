package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreatePromotionRequest;
import com.institutojf.mottainai.dto.request.UpdatePromotionRequest;
import com.institutojf.mottainai.dto.response.PromotionResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.enums.PromotionStatus;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    private final RetailStoreRepository retailStoreRepository;

    private final InventoryAccess inventoryAccess;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public PromotionResponse createPromotion(CreatePromotionRequest request, Authentication authentication) {
        inventoryAccess.checkStoreAccess(authentication, request.storeId());
        AppUser actor = requireRole(authentication, "MANAGER");
        RetailStore store = retailStoreRepository.findById(request.storeId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        Promotion promotion = new Promotion();
        promotion.setStore(store);
        promotion.setName(request.name());
        promotion.setDescription(request.description());
        promotion.setPromotionType(request.promotionType());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setStatus(PromotionStatus.PENDING_APPROVAL);
        promotion.setActive(false);
        promotion.setCreatedBy(actor.getEmployee());
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        promotion = promotionRepository.save(promotion);
        auditLogRepository.record("promotion", "INSERT", promotion.getId().toString(), actor.getId(), null,
                toAuditData(promotion));
        return PromotionResponse.fromEntity(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> getPromotionsByStore(Integer storeId, Authentication authentication) {
        inventoryAccess.checkStoreAccess(authentication, storeId);
        return promotionRepository.findByStore_IdAndDeletedAtIsNullOrderByStartsAtDesc(storeId)
            .stream()
            .map(PromotionResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Integer id, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(id, authentication);
        return PromotionResponse.fromEntity(promotion);
    }

    @Transactional
    public PromotionResponse updatePromotion(Integer id, UpdatePromotionRequest request, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(id, authentication);
        AppUser actor = requireRole(authentication, "MANAGER");
        ensurePendingApproval(promotion);
        PromotionAudit oldData = toAuditData(promotion);
        if (request.name() != null)
            promotion.setName(request.name());
        if (request.description() != null)
            promotion.setDescription(request.description());
        if (request.promotionType() != null)
            promotion.setPromotionType(request.promotionType());
        if (request.startsAt() != null)
            promotion.setStartsAt(request.startsAt());
        if (request.endsAt() != null)
            promotion.setEndsAt(request.endsAt());
        if (request.active() != null) {
            throw new BusinessException("Promotion active status can only be changed through the approval workflow");
        }
        promotion.setUpdatedAt(LocalDateTime.now());
        promotion = promotionRepository.save(promotion);
        auditLogRepository.record("promotion", "UPDATE", promotion.getId().toString(), actor.getId(), oldData,
                toAuditData(promotion));
        return PromotionResponse.fromEntity(promotion);
    }

    @Transactional
    public PromotionResponse approvePromotion(Integer id, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(id, authentication);
        AppUser actor = requireRole(authentication, "ADMINISTRATOR");
        ensurePendingApproval(promotion);
        PromotionAudit oldData = toAuditData(promotion);
        promotion.setActive(true);
        promotion.setStatus(PromotionStatus.APPROVED);
        promotion.setApprovedBy(actor.getEmployee());
        promotion.setApprovedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        promotion = promotionRepository.save(promotion);
        auditLogRepository.record("promotion", "UPDATE", promotion.getId().toString(), actor.getId(), oldData,
                toAuditData(promotion));
        return PromotionResponse.fromEntity(promotion);
    }

    @Transactional
    public PromotionResponse rejectPromotion(Integer id, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(id, authentication);
        AppUser actor = requireRole(authentication, "ADMINISTRATOR");
        ensurePendingApproval(promotion);
        PromotionAudit oldData = toAuditData(promotion);
        promotion.setActive(false);
        promotion.setStatus(PromotionStatus.REJECTED);
        promotion.setApprovedBy(null);
        promotion.setApprovedAt(null);
        promotion.setUpdatedAt(LocalDateTime.now());
        promotion = promotionRepository.save(promotion);
        auditLogRepository.record("promotion", "UPDATE", promotion.getId().toString(), actor.getId(), oldData,
                toAuditData(promotion));
        return PromotionResponse.fromEntity(promotion);
    }

    @Transactional
    public PromotionResponse cancelPromotion(Integer id, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(id, authentication);
        AppUser actor = requireRole(authentication, "ADMINISTRATOR");
        if (promotion.getStatus() == PromotionStatus.CANCELLED || promotion.getStatus() == PromotionStatus.REJECTED
                || promotion.getStatus() == PromotionStatus.EXPIRED) {
            throw new BusinessException("Promotion cannot be cancelled from its current status");
        }
        PromotionAudit oldData = toAuditData(promotion);
        promotion.setActive(false);
        promotion.setStatus(PromotionStatus.CANCELLED);
        promotion.setUpdatedAt(LocalDateTime.now());
        promotion = promotionRepository.save(promotion);
        auditLogRepository.record("promotion", "UPDATE", promotion.getId().toString(), actor.getId(), oldData,
                toAuditData(promotion));
        return PromotionResponse.fromEntity(promotion);
    }

    @Transactional
    public void deletePromotion(Integer id, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(id, authentication);
        AppUser actor = requireRole(authentication, "MANAGER");
        PromotionAudit oldData = toAuditData(promotion);
        promotion.setActive(false);
        promotion.setDeletedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
        auditLogRepository.record("promotion", "DELETE", promotion.getId().toString(), actor.getId(), oldData,
                toAuditData(promotion));
    }

    private Promotion findAccessiblePromotion(Integer id, Authentication authentication) {
        Promotion promotion = promotionRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        inventoryAccess.checkStoreAccess(authentication, promotion.getStore().getId());
        return promotion;
    }

    private AppUser requireRole(Authentication authentication, String roleName) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        if (authentication.getAuthorities().stream()
                .noneMatch(authority -> ("ROLE_" + roleName).equals(authority.getAuthority()))) {
            throw new BusinessException(
                    roleName.substring(0, 1) + roleName.substring(1).toLowerCase() + " access is required");
        }
        return actor;
    }

    private void ensurePendingApproval(Promotion promotion) {
        if (promotion.getStatus() != PromotionStatus.PENDING_APPROVAL) {
            throw new BusinessException("Promotion is not pending approval");
        }
    }

    private PromotionAudit toAuditData(Promotion promotion) {
        return new PromotionAudit(promotion.getStore().getId(), promotion.getName(), promotion.getPromotionType(),
                promotion.getStartsAt(), promotion.getEndsAt(), promotion.getStatus(), promotion.getActive(),
                promotion.getCreatedBy() == null ? null : promotion.getCreatedBy().getId(),
                promotion.getApprovedBy() == null ? null : promotion.getApprovedBy().getId());
    }

    private record PromotionAudit(Integer storeId, String name, String promotionType, LocalDateTime startsAt, LocalDateTime endsAt, PromotionStatus status, Boolean active, Integer createdBy, Integer approvedBy) {
    }

}
