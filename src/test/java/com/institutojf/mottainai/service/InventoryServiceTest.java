package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateInventoryRequest;
import com.institutojf.mottainai.dto.request.UpdateInventoryRequest;
import com.institutojf.mottainai.dto.response.InventoryResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.mapper.InventoryMapper;
import com.institutojf.mottainai.model.Batch;
import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.enums.InventoryType;
import com.institutojf.mottainai.repository.BatchRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InventoryService service;

    @Test
    @DisplayName("Should use store resolved from authentication when listing")
    void shouldUseStoreResolvedFromAuthenticationWhenListing() {
        when(inventoryAccess.resolveStoreId(authentication, 99)).thenReturn(2);
        when(inventoryRepository.findAllByStore_IdAndDeletedAtIsNull(2)).thenReturn(List.of());
        service.findAll(99, authentication);
        verify(inventoryRepository).findAllByStore_IdAndDeletedAtIsNull(2);
    }

    @Test
    @DisplayName("Should reject update when maximum is below minimum")
    void shouldRejectUpdateWhenMaximumIsBelowMinimum() {
        assertThrows(BusinessException.class, () -> service.update(1,
                new UpdateInventoryRequest(new BigDecimal("3.000"), new BigDecimal("2.000"), null, 1), authentication));
    }

    @Test
    @DisplayName("Should mark inventory as deleted without removing its row")
    void shouldSoftDeleteInventory() {
        RetailStore store = new RetailStore();
        store.setId(2);
        Inventory inventory = new Inventory();
        inventory.setStore(store);
        when(inventoryRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(inventory));
        service.deactivate(1, authentication);
        assertNotNull(inventory.getDeletedAt());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("Should reactivate a deleted inventory and preserve its balance and settings")
    void shouldReactivateExistingInventory() {
        RetailStore store = new RetailStore();
        store.setId(2);

        Batch batch = new Batch();
        batch.setId(3);

        Inventory existing = new Inventory();
        existing.setId(4);
        existing.setStore(store);
        existing.setBatch(batch);
        existing.setCurrentQuantity(new BigDecimal("10.000"));
        existing.setMinimumQuantity(new BigDecimal("2.000"));
        existing.setMaximumQuantity(new BigDecimal("20.000"));
        existing.setLocation("A");
        existing.setDeletedAt(LocalDateTime.now());

        CreateInventoryRequest request = new CreateInventoryRequest(null, 3, InventoryType.NORMAL, new BigDecimal("3.000"), new BigDecimal("30.000"), "B");
        InventoryResponse response = new InventoryResponse(4, 2, 3, InventoryType.NORMAL, new BigDecimal("10.000"), new BigDecimal("2.000"), new BigDecimal("20.000"), "A", 2, true);

        when(inventoryAccess.resolveStoreId(authentication, null)).thenReturn(2);
        when(retailStoreRepository.findByIdAndActiveTrueAndDeletedAtIsNull(2)).thenReturn(Optional.of(store));
        when(batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(3)).thenReturn(Optional.of(batch));
        when(inventoryRepository.findByStore_IdAndBatch_IdAndInventoryType(2, 3, InventoryType.NORMAL))
            .thenReturn(Optional.of(existing));
        when(inventoryRepository.saveAndFlush(existing)).thenReturn(existing);
        when(inventoryMapper.toResponse(existing, true)).thenReturn(response);

        InventoryResponse result = service.create(request, authentication);

        assertEquals(response, result);
        assertNull(existing.getDeletedAt());
        assertEquals(new BigDecimal("10.000"), existing.getCurrentQuantity());
        assertEquals(new BigDecimal("2.000"), existing.getMinimumQuantity());
        assertEquals(new BigDecimal("20.000"), existing.getMaximumQuantity());
        assertEquals("A", existing.getLocation());

        verify(inventoryRepository, never()).save(existing);
    }

    @Test
    @DisplayName("Should reject creating an inventory that is still active")
    void shouldRejectExistingActiveInventory() {
        RetailStore store = new RetailStore();
        store.setId(2);

        Batch batch = new Batch();
        batch.setId(3);

        Inventory existing = new Inventory();

        CreateInventoryRequest request = new CreateInventoryRequest(null, 3, InventoryType.NORMAL, BigDecimal.ZERO, null, null);

        when(inventoryAccess.resolveStoreId(authentication, null)).thenReturn(2);
        when(retailStoreRepository.findByIdAndActiveTrueAndDeletedAtIsNull(2)).thenReturn(Optional.of(store));
        when(batchRepository.findByIdAndActiveTrueAndDeletedAtIsNull(3)).thenReturn(Optional.of(batch));
        when(inventoryRepository.findByStore_IdAndBatch_IdAndInventoryType(2, 3, InventoryType.NORMAL))
            .thenReturn(Optional.of(existing));
        assertThrows(ConflictException.class, () -> service.create(request, authentication));
        verify(inventoryRepository, never()).saveAndFlush(existing);
    }

}
