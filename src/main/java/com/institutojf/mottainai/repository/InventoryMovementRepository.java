package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Integer> {
    List<InventoryMovement> findAllByInventory_IdAndMovementDateBetweenOrderByMovementDateDesc(Integer inventoryId, LocalDateTime from, LocalDateTime to);

    List<InventoryMovement> findAllByStoreIdAndMovementDateBetweenOrderByMovementDateDesc(Integer storeId, LocalDateTime from, LocalDateTime to);
}
