package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    Optional<Address> findByIdAndDeletedAtIsNull(Integer id);

    Page<Address> findAllByDeletedAtIsNull(Pageable pageable);
    @Query("""
            SELECT COUNT(address) > 0
            FROM Address address
            WHERE address.deletedAt IS NULL
              AND address.zipCode = :zipCode
              AND address.street = :street
              AND address.number = :number
              AND COALESCE(address.complement, '') = COALESCE(:complement, '')
              AND (:excludedId IS NULL OR address.id <> :excludedId)
            """)
    boolean existsActiveAddress(
            String zipCode,
            String street,
            String number,
            String complement,
            Integer excludedId
    );
}
