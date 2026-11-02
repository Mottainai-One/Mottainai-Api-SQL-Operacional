package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.SystemEventResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.repository.*;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemMonitoringServiceTest {

    @Mock
    private SystemMonitoringRepository repository;

    @Mock
    private AuditLogRepository audit;

    @Mock
    private InventoryAccess access;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SystemMonitoringService service;

    @Test
    @DisplayName("Should retry only failed events and audit them")
    void retriesOnlyFailedEventAndAuditsIt() {
        AppUser actor = actor();
        SystemEventResponse failed = new SystemEventResponse(3L, null, "X", "x", "1", null, 5, "FAILED", 2, "error",
                LocalDateTime.now(), null);
        SystemEventResponse pending = new SystemEventResponse(3L, null, "X", "x", "1", null, 5, "PENDING", 3, null,
                LocalDateTime.now(), null);
        when(access.currentUser(authentication)).thenReturn(actor);
        when(repository.findEventForUpdate(3L, 4)).thenReturn(Optional.of(failed), Optional.of(pending));
        when(repository.retry(3L)).thenReturn(true);
        assertEquals("PENDING", service.retry(3L, authentication).status());
        verify(audit).record(eq("event_queue"), eq("UPDATE"), eq("3"), eq(8), any(), any());
    }

    @Test
    @DisplayName("Should reject invalid six-month range")
    void rejectsInvalidSixMonthRange() {
        LocalDateTime from = LocalDateTime.now().minusMonths(7);
        when(access.currentUser(authentication)).thenReturn(actor());
        assertThrows(BusinessException.class, () -> service.events(from, LocalDateTime.now(), null, authentication));
    }

    private AppUser actor() {
        Company company = new Company();
        company.setId(4);
        RetailStore store = new RetailStore();
        store.setCompany(company);
        Employee employee = new Employee();
        employee.setStore(store);
        AppUser actor = new AppUser();
        actor.setId(8);
        actor.setEmployee(employee);
        return actor;
    }

}
