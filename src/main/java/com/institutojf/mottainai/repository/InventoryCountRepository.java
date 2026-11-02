package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.InventoryCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    List<InventoryCount> findAllByStore_IdOrderByStartedAtDesc(Integer storeId);

    @Query("select distinct c from InventoryCount c join fetch c.store join fetch c.employee "
            + "left join fetch c.items i left join fetch i.inventory "
            + "where c.id = :id and (i.id is null or i.deletedAt is null)")
    Optional<InventoryCount> findWithItemsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InventoryCount c join fetch c.store join fetch c.employee where c.id = :id")
    Optional<InventoryCount> findByIdForUpdate(@Param("id") Long id);

}
