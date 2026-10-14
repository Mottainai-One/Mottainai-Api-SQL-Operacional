package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.RetailStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RetailStoreRepository extends JpaRepository<RetailStore, Integer> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCompany_IdAndActiveTrueAndDeletedAtIsNull(Integer companyId);

    long countByCompany_IdAndActiveTrueAndDeletedAtIsNull(Integer companyId);

    Page<RetailStore> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    @Query("select s from RetailStore s where s.active = true and s.deletedAt is null "
        + "and (:query = '' or lower(s.name) like lower(concat('%', :query, '%'))) "
        + "order by s.name asc, s.id asc")
    Page<RetailStore> findCustomerStores(@Param("query") String query, Pageable pageable);

    @Query(value = "select s.* from mottainai.retail_store s where s.active = true "
        + "and s.deleted_at is null and (:query = '' or lower(s.name) like concat('%', lower(:query), '%')) "
        + "and s.latitude is not null and s.longitude is not null "
        + "and 6371.0 * acos(least(1.0, greatest(-1.0, "
        + "cos(radians(:latitude)) * cos(radians(s.latitude)) "
        + "* cos(radians(s.longitude) - radians(:longitude)) "
        + "+ sin(radians(:latitude)) * sin(radians(s.latitude))))) <= :radiusKm "
        + "order by s.name asc, s.store_id asc",
        countQuery = "select count(*) from mottainai.retail_store s where s.active = true "
            + "and s.deleted_at is null and (:query = '' or lower(s.name) like concat('%', lower(:query), '%')) "
            + "and s.latitude is not null and s.longitude is not null "
            + "and 6371.0 * acos(least(1.0, greatest(-1.0, "
            + "cos(radians(:latitude)) * cos(radians(s.latitude)) "
            + "* cos(radians(s.longitude) - radians(:longitude)) "
            + "+ sin(radians(:latitude)) * sin(radians(s.latitude))))) <= :radiusKm",
        nativeQuery = true)
    Page<RetailStore> findCustomerStoresNear(@Param("query") String query,
        @Param("latitude") double latitude, @Param("longitude") double longitude,
        @Param("radiusKm") double radiusKm, Pageable pageable);

    Optional<RetailStore> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<RetailStore> findByIdAndDeletedAtIsNull(Integer id);

    List<RetailStore> findAllByCompany_IdAndDeletedAtIsNull(Integer companyId);

    boolean existsByIdAndCompany_IdAndActiveTrueAndDeletedAtIsNull(Integer id, Integer companyId);

}
