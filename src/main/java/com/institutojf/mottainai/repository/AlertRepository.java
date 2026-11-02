package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Alert;
import com.institutojf.mottainai.model.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> findByStore_IdOrderByGeneratedAtDesc(Integer storeId);

    long countByStore_IdAndStatus(Integer storeId, AlertStatus status);

}
