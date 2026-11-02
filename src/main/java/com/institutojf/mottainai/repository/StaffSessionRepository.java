package com.institutojf.mottainai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StaffSessionRepository {
    private final JdbcTemplate jdbcTemplate;

    public void create(UUID sessionId, Integer userId, String tokenHash, OffsetDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO mottainai.staff_session
                    (session_id, user_id, refresh_token_hash, refresh_expires_at)
                VALUES (?, ?, ?, ?)
                """, sessionId, userId, tokenHash, expiresAt);
    }

    public boolean rotate(UUID sessionId, Integer userId, String oldHash, String newHash, OffsetDateTime expiresAt) {
        return jdbcTemplate.update("""
                UPDATE mottainai.staff_session
                   SET refresh_token_hash = ?, refresh_expires_at = ?, last_used_at = CURRENT_TIMESTAMP
                 WHERE session_id = ? AND user_id = ? AND refresh_token_hash = ?
                   AND revoked_at IS NULL AND refresh_expires_at > CURRENT_TIMESTAMP
                """, newHash, expiresAt, sessionId, userId, oldHash) == 1;
    }

    public boolean isActive(UUID sessionId, Integer userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM mottainai.staff_session
                 WHERE session_id = ? AND user_id = ?
                   AND revoked_at IS NULL AND refresh_expires_at > CURRENT_TIMESTAMP
                """, Integer.class, sessionId, userId);
        return count != null && count == 1;
    }

    public boolean revoke(UUID sessionId, String tokenHash, Integer userId) {
        return jdbcTemplate.update("""
                UPDATE mottainai.staff_session SET revoked_at = CURRENT_TIMESTAMP
                 WHERE session_id = ? AND refresh_token_hash = ? AND user_id = ? AND revoked_at IS NULL
                """, sessionId, tokenHash, userId) == 1;
    }

    public void revokeAllForUser(Integer userId) {
        jdbcTemplate.update("""
                UPDATE mottainai.staff_session SET revoked_at = CURRENT_TIMESTAMP
                 WHERE user_id = ? AND revoked_at IS NULL
                """, userId);
    }
}
