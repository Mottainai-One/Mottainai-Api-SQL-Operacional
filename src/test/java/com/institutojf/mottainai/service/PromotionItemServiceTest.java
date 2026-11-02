package com.institutojf.mottainai.service;

import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.PromotionItem;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.PromotionItemRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionItemServiceTest {

    @Mock
    private PromotionItemRepository promotionItemRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PromotionItemService service;

    @Test
    @DisplayName("Should not delete item from another promotion")
    void shouldNotDeleteItemFromAnotherPromotion() {
        Promotion promotion = new Promotion();
        promotion.setId(7);
        PromotionItem item = new PromotionItem();
        item.setPromotion(promotion);
        when(promotionItemRepository.findByIdAndDeletedAtIsNull(5)).thenReturn(Optional.of(item));
        assertThrows(ResourceNotFoundException.class, () -> service.deletePromotionItem(9, 5, authentication));
        verify(inventoryAccess, never()).checkStoreAccess(authentication, 7);
        verify(promotionItemRepository, never()).save(item);
    }

}
