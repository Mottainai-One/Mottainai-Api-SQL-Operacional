package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {

    Optional<SubscriptionPlan> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<SubscriptionPlan> findByIdAndDeletedAtIsNull(Integer id);

    Page<SubscriptionPlan> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

}
