package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.BatchStorePriceItemRequest;
import com.institutojf.mottainai.dto.request.BatchStorePriceRequest;
import com.institutojf.mottainai.dto.response.StoreProductPriceResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.ProductCommercialRepository;
import com.institutojf.mottainai.repository.ProductRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCommercialServiceTest {

    @Mock
    private ProductCommercialRepository commercialRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RetailStoreRepository storeRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductCommercialService service;

    @Test
    @DisplayName("Should create store price and audit in the same service transaction")
    void shouldCreateStorePriceAndAuditInTheSameServiceTransaction() {
        Product product = new Product();
        RetailStore store = new RetailStore();
        AppUser actor = new AppUser();
        actor.setId(7);
        StoreProductPriceResponse saved = new StoreProductPriceResponse(3L, 2, 1, new BigDecimal("12.50"), LocalDateTime.now(), null, true, 1);
        when(inventoryAccess.currentUser(authentication)).thenReturn(actor);
        when(productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(product));
        when(storeRepository.findByIdAndActiveTrueAndDeletedAtIsNull(2)).thenReturn(Optional.of(store));
        when(commercialRepository.findStorePriceForUpdate(1, 2)).thenReturn(Optional.empty());
        when(commercialRepository.saveStorePrice(1, 2, new BigDecimal("12.50"), 7)).thenReturn(saved);
        StoreProductPriceResponse response = service.updateStorePrice(1, 2, new BigDecimal("12.50"), authentication);
        assertEquals(saved, response);
        verify(inventoryAccess).checkStoreAccess(authentication, 2);
        verify(auditLogRepository).record("store_product_price", "INSERT", "3", 7, null, saved);
    }

    @Test
    void shouldRejectDuplicateProductStorePairBeforeUpdatingBatch() {
        BatchStorePriceItemRequest first = new BatchStorePriceItemRequest(1, 2, new BigDecimal("10.00"));
        BatchStorePriceItemRequest second = new BatchStorePriceItemRequest(1, 2, new BigDecimal("11.00"));
        assertThrows(BusinessException.class, () -> service.updateBatch(new BatchStorePriceRequest(List.of(first, second)), authentication));
        verify(commercialRepository, never()).saveStorePrice(1, 2, new BigDecimal("10.00"), null);
    }

    @Test
    void shouldRejectHistoryRangeLongerThanSixMonths() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(new Product()));
        assertThrows(BusinessException.class, () -> service.findPriceHistory(1, from, from.plusMonths(6).plusDays(1)));
        verify(commercialRepository, never()).findPriceHistory(1, from, from.plusMonths(6).plusDays(1));
    }

}
