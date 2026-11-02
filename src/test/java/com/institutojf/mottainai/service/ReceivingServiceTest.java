package com.institutojf.mottainai.service;

import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.enums.ReceivingStatus;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.PurchaseOrderRepository;
import com.institutojf.mottainai.repository.ReceivingRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceivingServiceTest {

    @Mock
    private ReceivingRepository receivingRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReceivingService service;

    @Test
    @DisplayName("Should not credit inventory when receiving has differences")
    void shouldNotCreditInventoryWhenReceivingHasDifferences() {
        AppUser actor = new AppUser();
        actor.setId(2);
        Employee employee = new Employee();
        employee.setId(4);
        actor.setEmployee(employee);
        var item = new ReceivingRepository.ReceivingItemData(1, 7, 12, BigDecimal.TEN, new BigDecimal("9"), BigDecimal.ONE, LocalDate.now(), LocalDate.now().plusDays(10), null);
        var receiving = new ReceivingRepository.ReceivingData(5, 3, 8, 4, LocalDateTime.now(), ReceivingStatus.PENDING, null, LocalDateTime.now(), List.of(item));
        when(inventoryAccess.currentUser(authentication)).thenReturn(actor);
        when(receivingRepository.findByIdForUpdate(5)).thenReturn(Optional.of(receiving));
        assertThrows(BusinessException.class, () -> service.updateStatus(5, ReceivingStatus.CONFIRMED, authentication));
        verify(receivingRepository, never()).creditInventory(5, 8, 4);
    }

}
