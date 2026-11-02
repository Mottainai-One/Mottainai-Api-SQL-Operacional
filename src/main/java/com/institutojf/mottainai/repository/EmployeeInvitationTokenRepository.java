package com.institutojf.mottainai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmployeeInvitationTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public void create(Integer userId, String tokenHash, OffsetDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO mottainai.password_reset_token
                    (user_id, token_hash, token_type, expires_at)
                VALUES (?, ?, 'EMPLOYEE_INVITATION', ?)
                """, userId, tokenHash, expiresAt);
    }

    public Optional<Invitation> findUnusedByHashForUpdate(String tokenHash) {
        List<Invitation> invitations = jdbcTemplate.query("""
                SELECT recovery_token_id, user_id, expires_at
                  FROM mottainai.password_reset_token
                 WHERE token_hash = ? AND token_type = 'EMPLOYEE_INVITATION' AND used_at IS NULL
                 FOR UPDATE
                """,
                (resultSet, rowNumber) -> new Invitation(resultSet.getLong("recovery_token_id"),
                        resultSet.getInt("user_id"), resultSet.getObject("expires_at", OffsetDateTime.class)),
                tokenHash);
        return invitations.stream().findFirst();
    }

    public boolean existsUnusedByHash(String tokenHash) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM mottainai.password_reset_token
                 WHERE token_hash = ? AND token_type = 'EMPLOYEE_INVITATION'
                   AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                """, Integer.class, tokenHash);
        return count != null && count == 1;
    }

    public void markUsed(Long invitationId) {
        jdbcTemplate.update("""
                UPDATE mottainai.password_reset_token SET used_at = CURRENT_TIMESTAMP
                 WHERE recovery_token_id = ? AND used_at IS NULL
                """, invitationId);
    }

    public void invalidateUnusedForUser(Integer userId) {
        jdbcTemplate.update("""
                UPDATE mottainai.password_reset_token SET used_at = CURRENT_TIMESTAMP
                 WHERE user_id = ? AND token_type = 'EMPLOYEE_INVITATION' AND used_at IS NULL
                """, userId);
    }

    public boolean hasPendingForUser(Integer userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM mottainai.password_reset_token
                 WHERE user_id = ? AND token_type = 'EMPLOYEE_INVITATION' AND used_at IS NULL
                """, Integer.class, userId);
        return count != null && count > 0;
    }

    public boolean hasRecentForUser(Integer userId, OffsetDateTime since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM mottainai.password_reset_token
                 WHERE user_id = ? AND token_type = 'EMPLOYEE_INVITATION' AND created_at >= ?
                """, Integer.class, userId, since);
        return count != null && count > 0;
    }

    public record Invitation(
            Long id,
            Integer userId,
            OffsetDateTime expiresAt
    ) {
    }

}
