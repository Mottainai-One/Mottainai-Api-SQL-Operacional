package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateInventoryMovementRequest;
import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.mapper.InventoryMovementMapper;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.InventoryMovement;
import com.institutojf.mottainai.model.enums.MovementType;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.InventoryMovementRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryMovementServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private InventoryMovementMapper inventoryMovementMapper;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InventoryMovementService service;

    @Test
    @DisplayName("Should persist balances when movement is valid")
    void shouldPersistBalancesWhenMovementIsValid() {
        Inventory inventory = inventoryWithBalance("2.000");
        AppUser user = new AppUser();
        user.setEmployee(new Employee());
        InventoryMovementResponse response = new InventoryMovementResponse(1, 1, null, null, MovementType.IN, new BigDecimal("3.000"), new BigDecimal("2.000"), new BigDecimal("5.000"), null, 2);
        when(inventoryRepository.findActiveByIdForUpdate(1)).thenReturn(Optional.of(inventory));
        when(inventoryAccess.currentUser(authentication)).thenReturn(user);
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(invocation -> {
            InventoryMovement movement = invocation.getArgument(0);
            movement.setMovementId(1);
            return movement;
        });
        when(inventoryMovementMapper.toResponse(any(InventoryMovement.class))).thenReturn(response);
        InventoryMovementResponse result = service.create(1,
                new CreateInventoryMovementRequest(MovementType.IN, new BigDecimal("3.000"), null), authentication);
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryRepository).save(inventory);
        verify(inventoryMovementRepository).save(captor.capture());
        assertEquals(new BigDecimal("5.000"), inventory.getCurrentQuantity());
        assertEquals(new BigDecimal("2.000"), captor.getValue().getPreviousBalance());
        assertEquals(new BigDecimal("5.000"), captor.getValue().getCurrentBalance());
        assertEquals(response, result);
    }

    @Test
    @DisplayName("Should reject movement when balance would become negative")
    void shouldRejectMovementWhenBalanceWouldBecomeNegative() {
        Inventory inventory = inventoryWithBalance("2.000");
        when(inventoryRepository.findActiveByIdForUpdate(1)).thenReturn(Optional.of(inventory));
        assertThrows(BusinessException.class, () -> service.create(1,
                new CreateInventoryMovementRequest(MovementType.OUT, new BigDecimal("-3.000"), null), authentication));
        verify(inventoryRepository, never()).save(any());
        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject invalid direction for movement type")
    void shouldRejectInvalidDirectionForMovementType() {
        Inventory inventory = inventoryWithBalance("2.000");
        when(inventoryRepository.findActiveByIdForUpdate(1)).thenReturn(Optional.of(inventory));
        assertThrows(BusinessException.class, () -> service.create(1,
                new CreateInventoryMovementRequest(MovementType.IN, new BigDecimal("-1.000"), null), authentication));
        verify(inventoryRepository, never()).save(any());
    }

    private Inventory inventoryWithBalance(String balance) {
        RetailStore store = new RetailStore();
        store.setId(2);
        Company company = new Company();
        company.setId(3);
        store.setCompany(company);
        Inventory inventory = new Inventory();
        inventory.setId(1);
        inventory.setStore(store);
        inventory.setCurrentQuantity(new BigDecimal(balance));
        return inventory;
    }

}
