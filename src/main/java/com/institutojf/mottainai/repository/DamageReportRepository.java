package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.DamageReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {
    List<DamageReport> findAllByStore_IdOrderByCreatedAtDesc(Integer storeId);
}
