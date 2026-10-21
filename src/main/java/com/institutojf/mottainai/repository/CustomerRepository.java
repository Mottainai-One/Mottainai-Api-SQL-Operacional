package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByExternalAuthUid(String externalAuthUid);

    Optional<Customer> findByExternalAuthUidAndActiveTrueAndDeletedAtIsNull(String externalAuthUid);

    Optional<Customer> findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String email);

    Optional<Customer> findByIdAndDeletedAtIsNull(Integer id);

    List<Customer> findByDeletedAtIsNullOrderByIdAsc();

    boolean existsByCpfAndDeletedAtIsNull(String cpf);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

}
