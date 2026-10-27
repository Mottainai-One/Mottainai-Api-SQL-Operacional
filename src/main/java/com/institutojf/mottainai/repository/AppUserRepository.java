package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    @Query(value = "SELECT mottainai.fn_api_create_staff_user(:employeeId, :passwordHash, :active, :passwordSet)", nativeQuery = true)
    Integer createStaffUser(@Param("employeeId") Integer employeeId,
            @Param("passwordHash") String passwordHash,
            @Param("active") Boolean active,
            @Param("passwordSet") Boolean passwordSet);
    @Query("""
            SELECT COUNT(u) FROM AppUser u
            WHERE u.employee.store.company.id = :companyId
              AND u.deletedAt IS NULL
              AND u.employee.deletedAt IS NULL
              AND ((u.active = true AND u.employee.active = true)
                   OR (u.active = false AND u.passwordSet = false AND u.firebaseUid IS NULL))
            """)
    long countUserSeatsByCompanyId(@Param("companyId") Integer companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdWithWriteLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.firebaseUid = :firebaseUid "
            + "AND u.active = true AND u.deletedAt IS NULL")
    Optional<AppUser> findActiveByFirebaseUidWithWriteLock(@Param("firebaseUid") String firebaseUid);

    @Modifying
    @Query("UPDATE AppUser u SET u.lastLogin = :lastLogin WHERE u.id = :id")
    int updateLastLogin(@Param("id") Integer id, @Param("lastLogin") LocalDateTime lastLogin);

    /**
     * Busca usuários que podem fazer login, ou seja, que estão ativos e não foram
     * deletados
     */
    @EntityGraph(attributePaths = { "employee", "employee.privateData", "employee.role", "employee.store" })
    Optional<AppUser> findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(String firebaseUid);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByIdAndFirebaseUid(Integer id, String firebaseUid);

    @EntityGraph(attributePaths = { "employee", "employee.privateData", "employee.store", "employee.role" })
    Optional<AppUser> findByFirebaseUidAndDeletedAtIsNull(String firebaseUid);

    @EntityGraph(attributePaths = { "employee", "employee.privateData", "employee.store", "employee.role" })
    List<AppUser> findAllByDeletedAtIsNull();

    @Query("""
                SELECT u FROM AppUser u JOIN FETCH u.employee e JOIN FETCH e.role LEFT JOIN FETCH e.privateData
                JOIN FETCH e.store s WHERE e.store.id = :storeId
                AND e.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    List<AppUser> findEmployeesByStore(@Param("storeId") Integer storeId);

    @Query("""
                SELECT u FROM AppUser u JOIN FETCH u.employee e JOIN FETCH e.role LEFT JOIN FETCH e.privateData
                JOIN FETCH e.store s WHERE s.company.id = :companyId
                AND e.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    List<AppUser> findEmployeesByCompany(@Param("companyId") Integer companyId);

    List<AppUser> findAllByEmployee_Store_Company_IdAndDeletedAtIsNull(Integer companyId);

    @Query("""
                SELECT u FROM AppUser u JOIN FETCH u.employee e JOIN FETCH e.role LEFT JOIN FETCH e.privateData
                JOIN FETCH e.store WHERE e.id = :employeeId
                AND e.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    Optional<AppUser> findByEmployeeId(@Param("employeeId") Integer employeeId);

    @EntityGraph(attributePaths = { "employee", "employee.privateData", "employee.role", "employee.store" })
    Optional<AppUser> findByEmployee_Id(Integer employeeId);

}
