package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.PromotionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionItemRepository extends JpaRepository<PromotionItem, Integer> {

    List<PromotionItem> findByPromotion_IdAndDeletedAtIsNull(Integer promotionId);

    Optional<PromotionItem> findByIdAndDeletedAtIsNull(Integer id);

    Optional<PromotionItem> findByPromotion_IdAndProduct_IdAndDeletedAtIsNull(Integer promotionId, Integer productId);

    @Query("select i from PromotionItem i join fetch i.product where i.promotion.id = :promotionId "
        + "and i.deletedAt is null and i.quantityAvailable > 0 and i.product.active = true and i.product.deletedAt is null "
        + "order by i.id asc")
    List<PromotionItem> findAvailableCustomerItems(@Param("promotionId") Integer promotionId);

    @Query("select i from PromotionItem i join fetch i.product where i.promotion.id in :promotionIds "
        + "and i.deletedAt is null and i.quantityAvailable > 0 and i.product.active = true and i.product.deletedAt is null "
        + "order by i.id asc")
    List<PromotionItem> findAvailableCustomerItemsForPromotions(
        @Param("promotionIds") List<Integer> promotionIds);

}
