package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.SystemRule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface SystemRuleRepository extends JpaRepository<SystemRule, Integer> {

    List<SystemRule> findAllByOrderByCategoryAscKeyAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SystemRule> findAllByKey(String key);

}
