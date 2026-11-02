package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.BatchStorePriceItemRequest;
import com.institutojf.mottainai.dto.request.BatchStorePriceRequest;
import com.institutojf.mottainai.dto.response.ProductHistoryResponse;
import com.institutojf.mottainai.dto.response.ProductPriceHistoryResponse;
import com.institutojf.mottainai.dto.response.StoreProductPriceResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.ProductCommercialRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductCommercialService {

    private final ProductCommercialRepository productCommercialRepository;

    private final ProductRepository productRepository;

    private final RetailStoreRepository retailStoreRepository;

    private final InventoryAccess inventoryAccess;

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<StoreProductPriceResponse> findStorePrices(Integer productId) {
        requireProduct(productId);
        return productCommercialRepository.findStorePrices(productId);
    }

    @Transactional
    public StoreProductPriceResponse updateStorePrice(Integer productId, Integer storeId, java.math.BigDecimal price, Authentication authentication) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        requireProduct(productId);
        requireStore(storeId);
        inventoryAccess.checkStoreAccess(authentication, storeId);
        StoreProductPriceResponse oldPrice = productCommercialRepository.findStorePriceForUpdate(productId, storeId)
            .orElse(null);
        StoreProductPriceResponse updated = productCommercialRepository.saveStorePrice(productId, storeId, price, actor.getId());
        auditLogRepository.record("store_product_price", oldPrice == null ? "INSERT" : "UPDATE", updated.id().toString(), actor.getId(), oldPrice, updated);
        return updated;
    }

    @Transactional
    public List<StoreProductPriceResponse> updateBatch(BatchStorePriceRequest request, Authentication authentication) {
        Set<String> pairs = new HashSet<>();
        for (BatchStorePriceItemRequest price : request.prices()) {
            if (!pairs.add(price.productId() + ":" + price.storeId())) {
                throw new BusinessException("Batch contains a duplicate product and store pair");
            }
        }
        return request.prices()
            .stream()
            .map(price -> updateStorePrice(price.productId(), price.storeId(), price.newPrice(), authentication))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductHistoryResponse> findMasterHistory(Integer productId, LocalDateTime from, LocalDateTime to) {
        requireProduct(productId);
        validateRange(from, to);
        return productCommercialRepository.findMasterHistory(productId, from, to);
    }

    @Transactional(readOnly = true)
    public List<ProductPriceHistoryResponse> findPriceHistory(Integer productId, LocalDateTime from, LocalDateTime to) {
        requireProduct(productId);
        validateRange(from, to);
        return productCommercialRepository.findPriceHistory(productId, from, to);
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid history date range of at most six months is required");
        }
    }

    private void requireProduct(Integer productId) {
        productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void requireStore(Integer storeId) {
        retailStoreRepository.findByIdAndActiveTrueAndDeletedAtIsNull(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

}
