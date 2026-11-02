package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.*;
import com.institutojf.mottainai.exception.*;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.*;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemMonitoringService {

    private final SystemMonitoringRepository repository;

    private final AuditLogRepository auditLogRepository;

    private final InventoryAccess inventoryAccess;

    @Transactional(readOnly = true)
    public List<SystemEventResponse> events(LocalDateTime from, LocalDateTime to, String status, Authentication auth) {
        AppUser actor = requireAdministrator(auth);
        validateRange(from, to);
        return repository.findEvents(companyId(actor), from, to, normalize(status));
    }

    @Transactional
    public SystemEventResponse retry(Long id, Authentication auth) {
        AppUser actor = requireAdministrator(auth);
        Integer companyId = companyId(actor);
        SystemEventResponse event = repository.findEventForUpdate(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Outbox event not found"));
        if (!"FAILED".equals(event.status()) || !repository.retry(id))
            throw new BusinessException("Only failed events can be retried");
        auditLogRepository.record("event_queue", "UPDATE", id.toString(), actor.getId(),
                Map.of("status", event.status(), "retry_count", event.retryCount()),
                Map.of("status", "PENDING", "retry_count", event.retryCount() + 1));
        return repository.findEventForUpdate(id, companyId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<SystemLogResponse> logs(LocalDateTime from, LocalDateTime to, String level, Authentication auth) {
        AppUser actor = requireAdministrator(auth);
        validateRange(from, to);
        return repository.findLogs(companyId(actor), from, to, normalize(level));
    }

    @Transactional(readOnly = true)
    public List<SystemJobResponse> jobs(LocalDateTime from, LocalDateTime to, Boolean success, Authentication auth) {
        AppUser actor = requireAdministrator(auth);
        validateRange(from, to);
        return repository.findJobs(companyId(actor), from, to, success);
    }

    private AppUser requireAdministrator(Authentication auth) {
        inventoryAccess.requireAdministrator(auth);
        return inventoryAccess.currentUser(auth);
    }

    private Integer companyId(AppUser actor) {
        return actor.getEmployee().getStore().getCompany().getId();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid date range of at most six months is required");
        }
    }

}
