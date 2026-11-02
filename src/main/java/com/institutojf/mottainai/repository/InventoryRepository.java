package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Inventory;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.enums.InventoryType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Query(value = "SELECT mottainai.fn_atomic_update_inventory(:inventoryId, :quantity, CAST(:movementType AS mottainai.movement_type), :employeeId, :observation, :version)", nativeQuery = true)
    BigDecimal applyAtomicMovement(@Param("inventoryId") Integer inventoryId, @Param("quantity") BigDecimal quantity, @Param("movementType") String movementType, @Param("employeeId") Integer employeeId, @Param("observation") String observation, @Param("version") Integer version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.id = :id and i.deletedAt is null")
    Optional<Inventory> findActiveByIdForUpdate(@Param("id") Integer id);

    Optional<Inventory> findByIdAndDeletedAtIsNull(Integer id);

    /**
     * Lista os registros de inventário ativos e não excluídos de uma loja
     */
    List<Inventory> findAllByStore_IdAndDeletedAtIsNull(Integer storeId);

    /**
     * Lista os registros de inventário ativos e não excluídos de uma loja pelo código de
     * barras do produto do lote
     */
    List<Inventory> findAllByStore_IdAndBatch_Product_BarcodeAndDeletedAtIsNull(Integer storeId, String barcode);

    /**
     * Lista os registros de inventário ativos e não excluídos de uma loja em que a
     * validade do lote esteja no intervalo informado
     */
    List<Inventory> findAllByStore_IdAndBatch_ExpirationDateBetweenAndDeletedAtIsNull(Integer storeId,
            LocalDate startDate, LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByStore_IdAndBatch_IdAndInventoryType(Integer storeId, Integer batchId, InventoryType inventoryType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByStore_IdAndBatch_IdAndInventoryTypeAndDeletedAtIsNull(Integer storeId, Integer batchId, InventoryType inventoryType);

    @Query("""
            select i.batch.product as product, sum(i.minimumQuantity) - sum(i.currentQuantity) as shortage,
                   sum(i.currentQuantity) as currentQuantity, sum(i.minimumQuantity) as minimumQuantity
            from Inventory i
            where i.store.id = :storeId and i.deletedAt is null and i.batch.active = true
              and i.batch.deletedAt is null
            group by i.batch.product
            having sum(i.currentQuantity) < sum(i.minimumQuantity)
            """)
    List<ReplenishmentShortage> findReplenishmentShortages(@Param("storeId") Integer storeId);

    interface ReplenishmentShortage {

        Product getProduct();

        BigDecimal getShortage();

        BigDecimal getCurrentQuantity();

        BigDecimal getMinimumQuantity();

    }

}
