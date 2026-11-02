package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.ReplenishmentPreList;
import com.institutojf.mottainai.model.enums.PreListStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReplenishmentPreListRepository extends JpaRepository<ReplenishmentPreList, Integer> {

    @EntityGraph(attributePaths = { "items", "items.product" })
    List<ReplenishmentPreList> findByStore_IdAndStatusInOrderByGeneratedAtDesc(Integer storeId, List<PreListStatus> statuses);

    @EntityGraph(attributePaths = { "items", "items.product" })
    Optional<ReplenishmentPreList> findOneById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "items", "items.product" })
    @Query("select p from ReplenishmentPreList p where p.id = :id")
    Optional<ReplenishmentPreList> findByIdForUpdate(@Param("id") Integer id);

}
