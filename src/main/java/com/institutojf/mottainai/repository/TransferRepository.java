package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Transfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Integer> {

    @EntityGraph(attributePaths = { "items", "items.batch", "sourceStore", "destinationStore", "employee" })
    @Query("select distinct transfer from Transfer transfer where "
            + "(transfer.sourceStore.id = :storeId or transfer.destinationStore.id = :storeId) "
            + "and transfer.requestDate between :from and :to order by transfer.requestDate desc")
    List<Transfer> findHistory(@Param("storeId") Integer storeId, @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    Optional<Transfer> findByIdAndDestinationStore_Id(Integer id, Integer storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "items", "items.batch", "sourceStore", "destinationStore", "employee" })
    @Query("select distinct transfer from Transfer transfer where transfer.id = :id")
    Optional<Transfer> findByIdForUpdate(@Param("id") Integer id);

}
