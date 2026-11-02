package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Integer> {

    List<LoyaltyTransaction> findByLoyaltyAccount_IdAndCreatedAtBetweenOrderByCreatedAtDesc(Integer loyaltyAccountId,
            LocalDateTime from, LocalDateTime to);

}
