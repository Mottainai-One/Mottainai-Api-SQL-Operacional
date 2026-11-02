package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.UpdateDonationStatusRequest;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

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
    private DonationService service;

    @Test
    @DisplayName("Should debit stock only when donation is completed")
    void debitsStockOnlyWhenDonationIsCompleted() {
        Donation donation = donation();
        Inventory inventory = new Inventory();
        inventory.setId(30);

        when(donationRepository.findByIdForUpdate(5)).thenReturn(Optional.of(donation));

        AppUser actor = new AppUser();
        actor.setId(7);
        actor.setEmployee(donation.getEmployee());
        when(inventoryAccess.currentUser(authentication)).thenReturn(actor);
        when(inventoryRepository.findByStore_IdAndBatch_IdAndInventoryType(1, 9, InventoryType.NORMAL))
            .thenReturn(Optional.of(inventory));
        when(donationRepository.save(donation)).thenReturn(donation);
        service.updateStatus(5, new UpdateDonationStatusRequest(DonationStatus.COMPLETED), authentication);
        verify(movementService).create(eq(30), argThat(r -> r.movementType() == MovementType.DONATION && r.movedQuantity().compareTo(new BigDecimal("-2.000")) == 0), eq(authentication));
    }

    private Donation donation() {
        Company company = new Company();
        company.setId(2);

        RetailStore store = new RetailStore();
        store.setId(1);
        store.setCompany(company);

        Employee employee = new Employee();
        employee.setId(3);
        employee.setStore(store);

        Batch batch = new Batch();
        batch.setId(9);

        Donation donation = new Donation();
        donation.setId(5);
        donation.setStore(store);
        donation.setEmployee(employee);
        donation.setInstitution("ONG");
        donation.setDonationDate(LocalDateTime.now());

        DonationItem item = new DonationItem();
        item.setId(4);
        item.setBatch(batch);
        item.setDonatedQuantity(new BigDecimal("2.000"));
        donation.addItem(item);

        return donation;
    }

}
