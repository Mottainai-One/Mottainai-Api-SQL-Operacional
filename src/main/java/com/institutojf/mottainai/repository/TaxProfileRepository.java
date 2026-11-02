package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.TaxProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TaxProfileRepository extends JpaRepository<TaxProfile, Integer> {

    Optional<TaxProfile> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<TaxProfile> findByIdAndDeletedAtIsNull(Integer id);

    Optional<TaxProfile> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<TaxProfile> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

}
