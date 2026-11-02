package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<Company> findByIdAndDeletedAtIsNull(Integer id);

    Page<Company> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    boolean existsByCnpj(String cnpj);

    boolean existsByPlan_IdAndActiveTrueAndDeletedAtIsNull(Integer planId);
}
