package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateInventoryCountItemRequest;
import com.institutojf.mottainai.dto.request.CreateInventoryCountRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.InventoryCount;
import com.institutojf.mottainai.model.InventoryCountItem;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.enums.InventoryCountStatus;
import com.institutojf.mottainai.repository.InventoryCountItemRepository;
import com.institutojf.mottainai.repository.InventoryCountRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceTest {

    @Mock
    private InventoryCountRepository countRepository;

    @Mock
    private InventoryCountItemRepository itemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private EntityManager entityManager;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InventoryCountService service;

    @Test
    @DisplayName("Should create an In Progress count for the authenticated employee store")
    void createsAnInProgressCountForTheAuthenticatedEmployeeStore() {
        AppUser user = user(3, 7);
        when(inventoryAccess.currentUser(authentication)).thenReturn(user);
        when(countRepository.save(any())).thenAnswer(invocation -> {
            InventoryCount count = invocation.getArgument(0);
            count.setId(11L);
            return count;
        });
        var response = service.create(new CreateInventoryCountRequest("Morning count"), authentication);
        assertEquals(11L, response.id());
        assertEquals(7, response.storeId());
        assertEquals(InventoryCountStatus.IN_PROGRESS, response.status());
    }

    @Test
    @DisplayName("Should reject an inventory from another store")
    void rejectsAnInventoryFromAnotherStore() {
        InventoryCount count = count(11L, 7);
        Inventory inventory = new Inventory();
        inventory.setId(21);
        inventory.setStore(store(8));
        when(countRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(count));
        when(inventoryRepository.findActiveByIdForUpdate(21)).thenReturn(Optional.of(inventory));
        assertThrows(BusinessException.class, () -> service.addItem(11L, new CreateInventoryCountItemRequest(21, BigDecimal.ONE, null), authentication));
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject completion without items")
    void rejectsCompletionWithoutItems() {
        InventoryCount count = count(11L, 7);
        when(countRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(count));
        when(itemRepository.findAllByCountIdWithInventory(11L)).thenReturn(List.of());
        when(outboxEventRepository.findByIdempotencyKey("count-key")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.finish(11L, "count-key", authentication));
        verify(countRepository, never()).save(any());
        verify(inventoryRepository, never()).applyAtomicMovement(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return the completed count when the Idempotency Key is replayed")
    void returnsTheCompletedCountWhenTheIdempotencyKeyIsReplayed() {
        InventoryCount count = count(11L, 7);
        count.setStatus(InventoryCountStatus.COMPLETED);
        when(outboxEventRepository.findByIdempotencyKey("count-key"))
            .thenReturn(Optional.of(new OutboxEventRepository.Event("INVENTORY_COUNT_COMPLETED", "inventory_count", "11", "{}")));
        when(outboxEventRepository.isEventFor(any(), eq("INVENTORY_COUNT_COMPLETED"), eq("inventory_count")))
            .thenReturn(true);
        when(countRepository.findWithItemsById(11L)).thenReturn(Optional.of(count));
        var response = service.finish(11L, "count-key", authentication);
        assertEquals(InventoryCountStatus.COMPLETED, response.status());
        verify(inventoryRepository, never()).applyAtomicMovement(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should refresh inventory state before completing and publishing the count")
    void refreshesInventoryBeforeCompletingAndPublishingTheCount() {
        InventoryCount count = count(11L, 7);
        InventoryCountItem item = countedItem(count);
        when(outboxEventRepository.findByIdempotencyKey("count-key")).thenReturn(Optional.empty());
        when(countRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(count));
        when(itemRepository.findAllByCountIdWithInventory(11L)).thenReturn(List.of(item));
        when(inventoryAccess.currentUser(authentication)).thenReturn(user(3, 7));
        when(inventoryRepository.findActiveByIdForUpdate(21)).thenReturn(Optional.of(item.getInventory()));
        when(countRepository.saveAndFlush(count)).thenReturn(count);

        var response = service.finish(11L, "count-key", authentication);

        assertEquals(InventoryCountStatus.COMPLETED, response.status());
        InOrder order = inOrder(inventoryRepository, entityManager, countRepository, outboxEventRepository);
        order.verify(inventoryRepository).applyAtomicMovement(21, BigDecimal.ONE, "ADJUSTMENT", 3, "Inventory count 11", 2);
        order.verify(entityManager).refresh(item.getInventory());
        order.verify(countRepository).saveAndFlush(count);
        order.verify(outboxEventRepository).publish(eq("INVENTORY_COUNT_COMPLETED"), eq("inventory_count"), eq("11"), any(), eq(1), eq(7), eq("count-key"));
    }

    @Test
    @DisplayName("Should not complete or publish the count when atomic inventory adjustment fails")
    void doesNotCompleteOrPublishWhenAtomicAdjustmentFails() {
        InventoryCount count = count(11L, 7);
        InventoryCountItem item = countedItem(count);
        when(outboxEventRepository.findByIdempotencyKey("count-key")).thenReturn(Optional.empty());
        when(countRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(count));
        when(itemRepository.findAllByCountIdWithInventory(11L)).thenReturn(List.of(item));
        when(inventoryAccess.currentUser(authentication)).thenReturn(user(3, 7));
        when(inventoryRepository.findActiveByIdForUpdate(21)).thenReturn(Optional.of(item.getInventory()));
        when(inventoryRepository.applyAtomicMovement(21, BigDecimal.ONE, "ADJUSTMENT", 3, "Inventory count 11", 2))
            .thenThrow(new DataIntegrityViolationException("Inventory adjustment failed"));

        assertThrows(DataIntegrityViolationException.class, () -> service.finish(11L, "count-key", authentication));

        assertEquals(InventoryCountStatus.IN_PROGRESS, count.getStatus());
        verify(entityManager, never()).refresh(any());
        verify(countRepository, never()).saveAndFlush(any());
        verify(outboxEventRepository, never()).publish(any(), any(), any(), any(), any(), any(), any());
    }

    private InventoryCountItem countedItem(InventoryCount count) {
        Inventory inventory = new Inventory();
        inventory.setId(21);
        inventory.setVersion(2);
        inventory.setStore(count.getStore());
        InventoryCountItem item = new InventoryCountItem();
        item.setId(31L);
        item.setInventoryCount(count);
        item.setInventory(inventory);
        item.setSystemQuantity(BigDecimal.ONE);
        item.setCountedQuantity(new BigDecimal("2"));
        return item;
    }

    private InventoryCount count(Long id, Integer storeId) {
        InventoryCount count = new InventoryCount();
        count.setId(id);
        count.setStore(store(storeId));
        count.setEmployee(user(3, storeId).getEmployee());
        count.setStatus(InventoryCountStatus.IN_PROGRESS);
        return count;
    }

    private AppUser user(Integer employeeId, Integer storeId) {
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setStore(store(storeId));
        AppUser user = new AppUser();
        user.setEmployee(employee);
        return user;
    }

    private RetailStore store(Integer id) {
        RetailStore store = new RetailStore();
        store.setId(id);
        Company company = new Company();
        company.setId(1);
        store.setCompany(company);
        return store;
    }

}
