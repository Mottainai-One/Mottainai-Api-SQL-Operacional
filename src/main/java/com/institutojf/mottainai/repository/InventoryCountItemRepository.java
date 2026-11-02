package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.InventoryCountItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryCountItemRepository extends JpaRepository<InventoryCountItem, Long> {

    Optional<InventoryCountItem> findByInventoryCount_IdAndInventory_Id(Long countId, Integer inventoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryCountItem i join fetch i.inventory where i.id = :id and i.inventoryCount.id = :countId and i.deletedAt is null")
    Optional<InventoryCountItem> findByIdAndCountIdForUpdate(@Param("countId") Long countId, @Param("id") Long id);

    @Query("select i from InventoryCountItem i join fetch i.inventory where i.inventoryCount.id = :countId and i.deletedAt is null")
    List<InventoryCountItem> findAllByCountIdWithInventory(@Param("countId") Long countId);

}
