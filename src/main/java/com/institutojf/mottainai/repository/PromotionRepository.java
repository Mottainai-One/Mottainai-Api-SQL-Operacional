package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    List<Promotion> findByStore_IdAndDeletedAtIsNullOrderByStartsAtDesc(Integer storeId);

    Optional<Promotion> findByIdAndDeletedAtIsNull(Integer id);

}
