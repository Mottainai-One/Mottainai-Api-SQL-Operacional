package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.AuditLogResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import com.institutojf.mottainai.security.InventoryAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private final EmployeeService employeeService;

    private final InventoryAccess inventoryAccess;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByEmployee(Integer employeeId, LocalDateTime from, LocalDateTime to, String actorEmail) {
        employeeService.find(employeeId, actorEmail);
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid audit date range of at most six months is required");
        }
        return auditLogRepository.findByEmployeeIdAndOperationDateBetween(employeeId, from, to);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAll(LocalDateTime from, LocalDateTime to, String tableAffected, String operation, Authentication authentication) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid audit date range of at most six months is required");
        }
        Integer companyId = inventoryAccess.currentUser(authentication).getEmployee().getStore().getCompany().getId();
        return auditLogRepository.findAll(companyId, from, to, tableAffected, operation);
    }

}
