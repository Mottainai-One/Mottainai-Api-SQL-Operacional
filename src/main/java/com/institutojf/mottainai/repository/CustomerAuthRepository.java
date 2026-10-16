package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.CustomerAuth;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerAuthRepository extends JpaRepository<CustomerAuth, Integer> {

    @EntityGraph(attributePaths = "customer")
    Optional<CustomerAuth> findByLoginEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select auth from CustomerAuth auth join fetch auth.customer where lower(auth.loginEmail) = lower(:email)")
    Optional<CustomerAuth> findByLoginEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select auth from CustomerAuth auth join fetch auth.customer where auth.id = :id")
    Optional<CustomerAuth> findByIdForUpdate(@Param("id") Integer id);

    Optional<CustomerAuth> findByCustomer_Id(Integer customerId);

}
