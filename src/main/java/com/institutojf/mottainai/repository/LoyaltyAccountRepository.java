package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.LoyaltyAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Integer> {

    Optional<LoyaltyAccount> findByCustomer_Id(Integer customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM LoyaltyAccount account where account.customer.id = :customerId")
    Optional<LoyaltyAccount> findByCustomerIdForUpdate(@Param("customerId") Integer customerId);

}
