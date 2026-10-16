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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdWithWriteLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE LOWER(u.email) = LOWER(:email) "
            + "AND u.active = true AND u.deletedAt IS NULL")
    Optional<AppUser> findActiveByEmailWithWriteLock(@Param("email") String email);

    @Modifying
    @Query("UPDATE AppUser u SET u.lastLogin = :lastLogin WHERE u.id = :id")
    int updateLastLogin(@Param("id") Integer id, @Param("lastLogin") LocalDateTime lastLogin);

    @Query("""
                    SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM AppUser u JOIN u.employee e
                    WHERE u.id = :id AND LOWER(u.email) = LOWER(:email)
                    AND u.cpf = :cpf AND e.cpf = :cpf
                    AND u.active = :active AND e.active = :active
                    AND u.deletedAt IS NULL AND e.deletedAt IS NULL
            """)
    boolean matchesCurrentIdentity(@Param("id") Integer id, @Param("email") String email, @Param("cpf") String cpf,
            @Param("active") boolean active);

    /**
     * Busca usuários que podem fazer login, ou seja, que estão ativos e não foram
     * deletados
     */
    @EntityGraph(attributePaths = { "employee", "employee.role" })
    Optional<AppUser> findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByIdAndFirebaseUid(Integer id, String firebaseUid);

    @Modifying
    @Query("UPDATE AppUser u SET u.firebaseUid = :firebaseUid WHERE u.id = :id AND u.firebaseUid IS NULL")
    int linkFirebaseUidIfAbsent(@Param("id") Integer id, @Param("firebaseUid") String firebaseUid);

    @EntityGraph(attributePaths = { "employee", "employee.role" })
    Optional<AppUser> findByEmployee_CpfAndActiveTrueAndDeletedAtIsNull(String cpf);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = { "employee", "employee.store", "employee.role" })
    Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = { "employee", "employee.store", "employee.role" })
    List<AppUser> findAllByDeletedAtIsNull();

    @Query("""
                SELECT u FROM AppUser u JOIN FETCH u.employee e JOIN FETCH e.role
                JOIN FETCH e.store s WHERE e.store.id = :storeId
                AND e.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    List<AppUser> findEmployeesByStore(@Param("storeId") Integer storeId);

    @Query("""
                SELECT u FROM AppUser u JOIN FETCH u.employee e JOIN FETCH e.role
                JOIN FETCH e.store s WHERE s.company.id = :companyId
                AND e.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    List<AppUser> findEmployeesByCompany(@Param("companyId") Integer companyId);

    List<AppUser> findAllByEmployee_Store_Company_IdAndDeletedAtIsNull(Integer companyId);

    @Query("""
                SELECT u FROM AppUser u JOIN FETCH u.employee e JOIN FETCH e.role
                JOIN FETCH e.store WHERE e.id = :employeeId
                AND e.deletedAt IS NULL AND u.deletedAt IS NULL
            """)
    Optional<AppUser> findByEmployeeId(@Param("employeeId") Integer employeeId);

}
