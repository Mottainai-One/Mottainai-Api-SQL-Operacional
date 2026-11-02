package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Disposal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DisposalRepository extends JpaRepository<Disposal, Integer> {

    @EntityGraph(attributePaths = { "items", "items.batch" })
    List<Disposal> findByStore_IdAndDisposalDateBetweenAndReasonContainingIgnoreCaseOrderByDisposalDateDesc(Integer storeId, LocalDateTime from, LocalDateTime to, String reason);

    @EntityGraph(attributePaths = { "items", "items.batch" })
    Optional<Disposal> findOneById(Integer id);

}
