package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.PromotionItemControllerApi;
import com.institutojf.mottainai.dto.request.CreatePromotionItemRequest;
import com.institutojf.mottainai.dto.request.UpdatePromotionItemRequest;
import com.institutojf.mottainai.dto.response.PromotionItemResponse;
import com.institutojf.mottainai.service.PromotionItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/{promotionId}/items")
@RequiredArgsConstructor
public class PromotionItemController implements PromotionItemControllerApi {

    private final PromotionItemService promotionItemService;

    @Override
    @GetMapping
    public List<PromotionItemResponse> getItemsByPromotion(@PathVariable Integer promotionId, Authentication authentication) {
        return promotionItemService.getItemsByPromotion(promotionId, authentication);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionItemResponse createPromotionItem(@PathVariable Integer promotionId, @Valid @RequestBody CreatePromotionItemRequest request, Authentication authentication) {
        return promotionItemService.createPromotionItem(promotionId, request, authentication);
    }

    @Override
    @PutMapping("/{id}")
    public PromotionItemResponse updatePromotionItem(@PathVariable Integer promotionId, @PathVariable Integer id, @Valid @RequestBody UpdatePromotionItemRequest request, Authentication authentication) {
        return promotionItemService.updatePromotionItem(promotionId, id, request, authentication);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromotionItem(@PathVariable Integer promotionId, @PathVariable Integer id, Authentication authentication) {
        promotionItemService.deletePromotionItem(promotionId, id, authentication);
    }

}
