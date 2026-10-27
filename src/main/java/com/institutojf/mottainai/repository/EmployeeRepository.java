package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findAllByStore_Company_IdAndDeletedAtIsNull(Integer companyId);

    boolean existsByCpfLookupHmac(byte[] cpfLookupHmac);

    boolean existsByCpfLookupHmacAndIdNot(byte[] cpfLookupHmac, Integer employeeId);

    boolean existsByEmailLookupHmac(byte[] emailLookupHmac);

    boolean existsByEmailLookupHmacAndIdNot(byte[] emailLookupHmac, Integer employeeId);

    @Query("SELECT e FROM Employee e JOIN FETCH e.role JOIN FETCH e.store s JOIN FETCH s.company "
            + "LEFT JOIN FETCH e.privateData WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Employee> findDirectoryById(@Param("id") Integer id);

    @Query("SELECT e FROM Employee e JOIN FETCH e.role JOIN FETCH e.store s JOIN FETCH s.company "
            + "LEFT JOIN FETCH e.privateData WHERE e.store.id = :storeId AND e.deletedAt IS NULL "
            + "ORDER BY e.id")
    List<Employee> findDirectoryByStoreId(@Param("storeId") Integer storeId);

    @Query("SELECT e FROM Employee e JOIN FETCH e.role JOIN FETCH e.store s JOIN FETCH s.company "
            + "LEFT JOIN FETCH e.privateData WHERE s.company.id = :companyId AND e.deletedAt IS NULL "
            + "ORDER BY e.id")
    List<Employee> findDirectoryByCompanyId(@Param("companyId") Integer companyId);

}
