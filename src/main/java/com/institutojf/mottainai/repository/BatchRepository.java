package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Integer> {

    Optional<Batch> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<Batch> findByIdAndDeletedAtIsNull(Integer id);

    boolean existsByBatchCode(String batchCode);

    List<Batch> findAllByActiveTrueAndDeletedAtIsNull();

}
