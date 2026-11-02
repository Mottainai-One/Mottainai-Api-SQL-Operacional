package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.ReplenishmentExecution;
import org.springframework.data.jpa.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReplenishmentExecutionRepository extends JpaRepository<ReplenishmentExecution, Integer> {

    @EntityGraph(attributePaths = { "preList", "items", "items.batch" })
    List<ReplenishmentExecution> findByPreList_Store_IdAndStartDateBetweenOrderByStartDateDesc(Integer storeId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "preList", "items", "items.batch" })
    Optional<ReplenishmentExecution> findOneById(Integer id);

}
