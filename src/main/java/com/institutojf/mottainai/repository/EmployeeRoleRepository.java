package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Integer> {
    Optional<EmployeeRole> findByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String name);

    Optional<EmployeeRole> findByIdAndDeletedAtIsNull(Integer id);

    List<EmployeeRole> findAllByActiveTrueAndDeletedAtIsNullOrderByPermissionLevelDescNameAsc();
}
