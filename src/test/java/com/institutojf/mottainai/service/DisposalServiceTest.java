package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateDisposalRequest;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.model.enums.*;
import com.institutojf.mottainai.repository.*;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisposalServiceTest {

    @Mock
    private DisposalRepository disposalRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementService movementService;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private OutboxEventRepository outbox;

    @Mock
    private AuditLogRepository audit;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DisposalService service;

    @Test
    @DisplayName("Should debit stock in same create flow")
    void debitsStockInSameCreateFlow() {
        AppUser actor = actor();

        Batch batch = new Batch();
        batch.setId(9);

        Inventory inventory = new Inventory();
        inventory.setId(30);

        var request = new CreateDisposalRequest("AVARIA", null, null,
                List.of(new CreateDisposalRequest.Item(9, new BigDecimal("2.000"))));
        when(inventoryAccess.currentUser(authentication)).thenReturn(actor);
        when(outbox.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(9)).thenReturn(Optional.of(batch));
        when(inventoryRepository.findByStore_IdAndBatch_IdAndInventoryType(1, 9, InventoryType.NORMAL))
            .thenReturn(Optional.of(inventory));
        when(disposalRepository.save(any())).thenAnswer(inv -> {
            Disposal d = inv.getArgument(0);
            d.setId(5);
            return d;
        });
        service.create(request, "key", authentication);

        verify(movementService).create(eq(30), argThat(r -> r.movementType() == MovementType.DISPOSAL && r.movedQuantity().compareTo(new BigDecimal("-2.000")) == 0), eq(authentication));
        verify(outbox).publish(eq("DISPOSAL_CREATED"), eq("disposal"), eq("5"), any(), eq(2), eq(1), eq("key"));
    }

    private AppUser actor() {
        Company c = new Company();
        c.setId(2);

        RetailStore s = new RetailStore();
        s.setId(1);
        s.setCompany(c);

        Employee e = new Employee();
        e.setId(3);
        e.setStore(s);

        AppUser u = new AppUser();
        u.setId(7);
        u.setEmployee(e);
        return u;
    }

}
