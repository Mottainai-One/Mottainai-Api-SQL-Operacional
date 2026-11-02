package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.AddressResponse;
import com.institutojf.mottainai.dto.response.CustomerCatalogPromotionResponse;
import com.institutojf.mottainai.dto.response.CustomerCatalogStoreResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.PromotionItem;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.enums.PromotionStatus;
import com.institutojf.mottainai.repository.PromotionItemRepository;
import com.institutojf.mottainai.repository.PromotionRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCatalogService {
    private final RetailStoreRepository storeRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionItemRepository itemRepository;

    @Transactional(readOnly = true)
    public Page<CustomerCatalogStoreResponse> stores(int page, int size, String query,
            Double latitude, Double longitude, Double radiusKm) {
        PageRequest pageable = pagination(page, size);
        String search = search(query);
        boolean geo = latitude != null || longitude != null || radiusKm != null;
        if (geo && (latitude == null || longitude == null || radiusKm == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude) || !Double.isFinite(radiusKm)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180
                || radiusKm <= 0 || radiusKm > 500)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "latitude, longitude and radiusKm must be supplied together with valid ranges");
        }
        Page<RetailStore> result = geo
            ? storeRepository.findCustomerStoresNear(search, latitude, longitude, radiusKm, pageable)
            : storeRepository.findCustomerStores(search, pageable);
        return result.map(this::storeResponse);
    }

    @Transactional(readOnly = true)
    public CustomerCatalogStoreResponse store(Integer id) {
        return storeResponse(storeRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found")));
    }

    @Transactional(readOnly = true)
    public Page<CustomerCatalogPromotionResponse> promotions(int page, int size, Integer storeId, String query) {
        if (storeId != null && storeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId must be positive");
        }
        Page<Promotion> result = promotionRepository.findCustomerPromotions(PromotionStatus.APPROVED,
            LocalDateTime.now(), storeId == null ? 0 : storeId, search(query), pagination(page, size));
        List<Integer> ids = result.getContent().stream().map(Promotion::getId).toList();
        Map<Integer, List<PromotionItem>> items = ids.isEmpty() ? Map.of()
            : itemRepository.findAvailableCustomerItemsForPromotions(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getPromotion().getId()));
        return result.map(promotion -> promotionResponse(promotion, items.getOrDefault(promotion.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public CustomerCatalogPromotionResponse promotion(Integer id) {
        Promotion promotion = promotionRepository.findCustomerPromotion(id, PromotionStatus.APPROVED,
            LocalDateTime.now()).orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        return promotionResponse(promotion, itemRepository.findAvailableCustomerItems(id));
    }

    private PageRequest pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be 1..100");
        }
        return PageRequest.of(page, size);
    }

    private String search(String query) {
        String value = query == null ? "" : query.trim();
        if (value.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must have at most 100 characters");
        }
        return value;
    }

    private CustomerCatalogStoreResponse storeResponse(RetailStore store) {
        var address = store.getAddress();
        return new CustomerCatalogStoreResponse(store.getId(), store.getName(),
            new AddressResponse(address.getId(), address.getZipCode(), address.getStreet(),
                address.getNumber(), address.getComplement(), address.getNeighborhood(),
                address.getCity(), address.getState()), store.getLatitude(), store.getLongitude());
    }

    private CustomerCatalogPromotionResponse promotionResponse(Promotion promotion, List<PromotionItem> items) {
        List<CustomerCatalogPromotionResponse.Item> responseItems = items.stream()
            .map(item -> new CustomerCatalogPromotionResponse.Item(item.getId(), item.getProduct().getId(),
                item.getProduct().getName(), item.getOriginalPrice(), item.getPromotionalPrice(),
                item.getQuantityAvailable()))
            .toList();
        return new CustomerCatalogPromotionResponse(promotion.getId(), promotion.getName(),
            promotion.getDescription(), promotion.getPromotionType(), promotion.getStartsAt(), promotion.getEndsAt(),
            storeResponse(promotion.getStore()), responseItems);
    }
}
