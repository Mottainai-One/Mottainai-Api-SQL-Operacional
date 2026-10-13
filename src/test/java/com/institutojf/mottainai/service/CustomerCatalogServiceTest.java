package com.institutojf.mottainai.service;

import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Address;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.PromotionItem;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.enums.PromotionStatus;
import com.institutojf.mottainai.repository.PromotionItemRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCatalogServiceTest {
    @Mock private RetailStoreRepository storeRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionItemRepository itemRepository;
    @InjectMocks private CustomerCatalogService service;

    @Test
    void storesWithoutLocationRemainInGeneralListing() {
        RetailStore store = store();
        when(storeRepository.findCustomerStores(eq("mercado"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(store)));

        var result = service.stores(0, 20, " mercado ", null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("Centro", result.getContent().getFirst().address().neighborhood());
        assertEquals(null, result.getContent().getFirst().latitude());
    }

    @Test
    void geographicSearchRequiresCompleteValidCoordinates() {
        assertThrows(ResponseStatusException.class,
            () -> service.stores(0, 20, null, -23.5, null, 10.0));
        assertThrows(ResponseStatusException.class,
            () -> service.stores(0, 20, null, -23.5, -46.6, 501.0));
    }

    @Test
    void promotionResponseContainsOnlyAvailableItemsReturnedByRepository() {
        Promotion promotion = new Promotion();
        promotion.setId(9);
        promotion.setName("Oferta de hoje");
        promotion.setPromotionType("SPECIAL_PRICE");
        promotion.setStore(store());
        promotion.setStartsAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        promotion.setEndsAt(LocalDateTime.of(2026, 10, 2, 18, 0));
        Product product = new Product();
        product.setId(7);
        product.setName("Arroz");
        PromotionItem item = new PromotionItem();
        item.setId(12);
        item.setPromotion(promotion);
        item.setProduct(product);
        item.setOriginalPrice(new BigDecimal("12.50"));
        item.setPromotionalPrice(new BigDecimal("9.90"));
        item.setQuantityAvailable(BigDecimal.ONE);
        when(promotionRepository.findCustomerPromotions(eq(PromotionStatus.APPROVED),
            any(LocalDateTime.class), eq(0), eq(""), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(promotion)));
        when(itemRepository.findAvailableCustomerItemsForPromotions(List.of(9))).thenReturn(List.of(item));

        var result = service.promotions(0, 20, null, null).getContent().getFirst();

        assertEquals("Arroz", result.items().getFirst().name());
        assertEquals(new BigDecimal("9.90"), result.items().getFirst().promotionalPrice());
        verify(promotionRepository).findCustomerPromotions(eq(PromotionStatus.APPROVED),
            any(LocalDateTime.class), eq(0), eq(""), any(Pageable.class));
    }

    @Test
    void promotionSearchPassesStoreFilterAndRequestedPageToRepository() {
        when(promotionRepository.findCustomerPromotions(eq(PromotionStatus.APPROVED),
            any(LocalDateTime.class), eq(42), eq("feijao"), eq(PageRequest.of(2, 10))))
            .thenReturn(Page.empty(PageRequest.of(2, 10)));

        service.promotions(2, 10, 42, " feijao ");

        verify(promotionRepository).findCustomerPromotions(eq(PromotionStatus.APPROVED),
            any(LocalDateTime.class), eq(42), eq("feijao"), eq(PageRequest.of(2, 10)));
    }

    @Test
    void unavailablePromotionIsNotExposedByDetail() {
        when(promotionRepository.findCustomerPromotion(eq(9), eq(PromotionStatus.APPROVED),
            any(LocalDateTime.class))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.promotion(9));
    }

    private RetailStore store() {
        Address address = new Address();
        address.setZipCode("01001000");
        address.setStreet("Rua Um");
        address.setNumber("10");
        address.setNeighborhood("Centro");
        address.setCity("Sao Paulo");
        address.setState("SP");
        RetailStore store = new RetailStore();
        store.setId(3);
        store.setName("Mercado");
        store.setAddress(address);
        return store;
    }
}
