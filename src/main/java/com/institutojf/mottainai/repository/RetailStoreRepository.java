package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.RetailStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailStoreRepository extends JpaRepository<RetailStore, Integer> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCompany_IdAndActiveTrueAndDeletedAtIsNull(Integer companyId);

    long countByCompany_IdAndActiveTrueAndDeletedAtIsNull(Integer companyId);

    Page<RetailStore> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    Optional<RetailStore> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<RetailStore> findByIdAndDeletedAtIsNull(Integer id);

    List<RetailStore> findAllByCompany_IdAndDeletedAtIsNull(Integer companyId);

    boolean existsByIdAndCompany_IdAndActiveTrueAndDeletedAtIsNull(Integer id, Integer companyId);

}
