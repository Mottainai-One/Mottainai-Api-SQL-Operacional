package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreatePromotionItemRequest;
import com.institutojf.mottainai.dto.request.UpdatePromotionItemRequest;
import com.institutojf.mottainai.dto.response.PromotionItemResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.PromotionItem;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.PromotionItemRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionItemService {

    private final PromotionItemRepository promotionItemRepository;

    private final PromotionRepository promotionRepository;

    private final ProductRepository productRepository;

    private final InventoryAccess inventoryAccess;

    @Transactional
    public PromotionItemResponse createPromotionItem(Integer promotionId, CreatePromotionItemRequest request, Authentication authentication) {
        Promotion promotion = findAccessiblePromotion(promotionId, authentication);
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (request.promotionalPrice().compareTo(request.originalPrice()) > 0) {
            throw new BusinessException("Promotional price cannot be greater than original price");
        }
        if (promotionItemRepository.findByPromotion_IdAndProduct_IdAndDeletedAtIsNull(promotionId, request.productId()).isPresent()) {
            throw new BusinessException("Product is already in this promotion");
        }
        PromotionItem item = new PromotionItem();
        item.setPromotion(promotion);
        item.setProduct(product);
        item.setOriginalPrice(request.originalPrice());
        item.setPromotionalPrice(request.promotionalPrice());
        item.setQuantityAvailable(request.quantityAvailable());
        item.setCreatedAt(LocalDateTime.now());
        return PromotionItemResponse.fromEntity(promotionItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<PromotionItemResponse> getItemsByPromotion(Integer promotionId, Authentication authentication) {
        findAccessiblePromotion(promotionId, authentication);
        return promotionItemRepository.findByPromotion_IdAndDeletedAtIsNull(promotionId)
            .stream()
            .map(PromotionItemResponse::fromEntity)
            .toList();
    }

    @Transactional
    public void deletePromotionItem(Integer promotionId, Integer id, Authentication authentication) {
        PromotionItem item = promotionItemRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion item not found"));
        if (!promotionId.equals(item.getPromotion().getId())) {
            throw new ResourceNotFoundException("Promotion item not found");
        }
        inventoryAccess.checkStoreAccess(authentication, item.getPromotion().getStore().getId());
        item.setDeletedAt(LocalDateTime.now());
        promotionItemRepository.save(item);
    }

    @Transactional
    public PromotionItemResponse updatePromotionItem(Integer promotionId, Integer id, UpdatePromotionItemRequest request, Authentication authentication) {
        PromotionItem item = promotionItemRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion item not found"));
        if (!promotionId.equals(item.getPromotion().getId())) {
            throw new ResourceNotFoundException("Promotion item not found");
        }
        inventoryAccess.checkStoreAccess(authentication, item.getPromotion().getStore().getId());
        if (request.promotionalPrice().compareTo(request.originalPrice()) > 0) {
            throw new BusinessException("Promotional price cannot be greater than original price");
        }
        item.setOriginalPrice(request.originalPrice());
        item.setPromotionalPrice(request.promotionalPrice());
        item.setQuantityAvailable(request.quantityAvailable());
        return PromotionItemResponse.fromEntity(promotionItemRepository.save(item));
    }

    private Promotion findAccessiblePromotion(Integer promotionId, Authentication authentication) {
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        inventoryAccess.checkStoreAccess(authentication, promotion.getStore().getId());
        return promotion;
    }

}
