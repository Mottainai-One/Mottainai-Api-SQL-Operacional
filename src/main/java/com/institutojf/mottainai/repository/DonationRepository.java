package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Donation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Integer> {

    @EntityGraph(attributePaths = { "items", "items.batch" })
    List<Donation> findByStore_IdAndDonationDateBetweenOrderByDonationDateDesc(Integer storeId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "items", "items.batch" })
    Optional<Donation> findOneById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "items", "items.batch" })
    @Query("select d from Donation d where d.id = :id")
    Optional<Donation> findByIdForUpdate(@Param("id") Integer id);

}
