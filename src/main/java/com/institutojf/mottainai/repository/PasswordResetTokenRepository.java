package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.PasswordResetToken;
import com.institutojf.mottainai.model.enums.PasswordTokenType;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.time.OffsetDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Busca o token ainda não usado pelo hash e bloqueia seu uso concorrente
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :hash "
            + "AND t.tokenType = :tokenType AND t.usedAt IS NULL")
    Optional<PasswordResetToken> findUnusedByHashForUpdate(@Param("hash") String hash, @Param("tokenType") PasswordTokenType tokenType);

    // Busca o último token ainda não usado para limitar novos pedidos
    Optional<PasswordResetToken> findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(Integer userId, PasswordTokenType tokenType);

    // Invalida tokens anteriores quando a senha é alterada
    @Modifying(flushAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :usedAt WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidateUnusedForUser(@Param("userId") Integer userId, @Param("usedAt") OffsetDateTime usedAt);
}
