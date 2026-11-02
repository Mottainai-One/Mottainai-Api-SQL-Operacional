package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.DecidePosApprovalRequest;
import com.institutojf.mottainai.dto.response.PosApprovalResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.PosApprovalStorageUnavailableException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.PosApprovalRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PosApprovalService {
    private final PosApprovalRepository requests;
    private final InventoryAccess access;
    private final AuditLogRepository audit;

    @Transactional(readOnly = true)
    public List<PosApprovalResponse> list(String status, int limit, Authentication authentication) {
        var actor = manager(authentication);
        if (!Set.of("PENDING", "APPROVED", "REJECTED", "EXECUTED").contains(status) || limit < 1 || limit > 100) {
            throw new BusinessException("Invalid approval status or limit");
        }
        return storage(() -> requests.list(actor.getEmployee().getStore().getId(), status, limit));
    }

    @Transactional
    public PosApprovalResponse decide(UUID id, DecidePosApprovalRequest decision, Authentication authentication) {
        var actor = manager(authentication);
        if (decision == null || decision.decision() == null
                || !Set.of("APPROVED", "REJECTED").contains(decision.decision())) {
            throw new BusinessException("Invalid approval decision");
        }
        Integer storeId = actor.getEmployee().getStore().getId();
        return storage(() -> {
            var request = requests.lock(id, storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("POS approval not found in this store"));
            if (!"PENDING".equals(request.status())) throw new ConflictException("Approval was already decided; reload it");
            if (!requests.validSaleStore(request)) throw new ConflictException("Approval sale does not belong to this store");
            boolean approve = "APPROVED".equals(decision.decision());
            boolean cancellation = Set.of("CANCEL_PURCHASE", "CANCEL_PRODUCT").contains(request.requestType());
            String status = decision.decision();
            if (cancellation) {
                if (request.cancellationRequestId() == null || !requests.validPendingCancellation(request)) {
                    throw new ConflictException("A matching pending cancellation is required");
                }
                requests.decideCancellation(request.cancellationRequestId(), actor.getEmployee().getId(),
                        approve, decision.observation());
                status = approve ? "EXECUTED" : "REJECTED";
            }
            var result = requests.saveDecision(id, storeId, status, actor.getEmployee().getId(),
                    actor.getFirebaseUid(), decision.observation());
            // Failure to record required audit rolls back the whole decision/cancellation.
            audit.record("pos_manager_approval_request", "UPDATE", id.toString(), actor.getId(),
                    Map.of("status", request.status()), Map.of("status", status, "decided_by_employee_id", actor.getEmployee().getId()));
            return result;
        });
    }

    private AppUser manager(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a ->
                Set.of("ROLE_ADMINISTRATOR", "ROLE_MANAGER").contains(a.getAuthority()))) {
            throw new AccessDeniedException("Manager access is required");
        }
        return access.currentUser(authentication);
    }

    private <T> T storage(Supplier<T> operation) {
        try { return operation.get(); }
        catch (DataAccessException error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql) {
                    if (sql.getSQLState() != null && Set.of("42P01", "42883").contains(sql.getSQLState())) {
                        throw new PosApprovalStorageUnavailableException(error);
                    }
                    if ("P0001".equals(sql.getSQLState())) throw new ConflictException("POS request changed; reload before deciding");
                }
            }
            throw error;
        }
    }
}
