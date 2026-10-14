package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Promotion;
import com.institutojf.mottainai.model.enums.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    List<Promotion> findByStore_IdAndDeletedAtIsNullOrderByStartsAtDesc(Integer storeId);

    Optional<Promotion> findByIdAndDeletedAtIsNull(Integer id);

    @Query("select p from Promotion p where p.status = :status and p.active = true "
        + "and p.deletedAt is null and p.store.active = true and p.store.deletedAt is null "
        + "and p.startsAt <= :now and p.endsAt > :now "
        + "and (:storeId = 0 or p.store.id = :storeId) "
        + "and (:query = '' or lower(p.name) like lower(concat('%', :query, '%'))) "
        + "and exists (select i.id from PromotionItem i where i.promotion = p "
        + "and i.quantityAvailable > 0 and i.product.active = true and i.product.deletedAt is null) "
        + "order by p.startsAt desc, p.id desc")
    Page<Promotion> findCustomerPromotions(@Param("status") PromotionStatus status,
        @Param("now") LocalDateTime now, @Param("storeId") Integer storeId,
        @Param("query") String query, Pageable pageable);

    @Query("select p from Promotion p where p.id = :id and p.status = :status and p.active = true "
        + "and p.deletedAt is null and p.store.active = true and p.store.deletedAt is null "
        + "and p.startsAt <= :now and p.endsAt > :now "
        + "and exists (select i.id from PromotionItem i where i.promotion = p "
        + "and i.quantityAvailable > 0 and i.product.active = true and i.product.deletedAt is null)")
    Optional<Promotion> findCustomerPromotion(@Param("id") Integer id,
        @Param("status") PromotionStatus status, @Param("now") LocalDateTime now);

}
