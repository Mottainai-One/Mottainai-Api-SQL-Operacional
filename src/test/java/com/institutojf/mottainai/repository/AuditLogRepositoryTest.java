package com.institutojf.mottainai.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditLogRepositoryTest {
    @Test void refusesFalseOrNullDatabaseConfirmation() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AuditLogRepository repository = new AuditLogRepository(jdbc, JsonMapper.builder().build());
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(false).thenReturn(null).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> repository.record("employee", "UPDATE", "505", 505, null, null));
        assertThrows(IllegalStateException.class, () -> repository.record("employee", "UPDATE", "505", 505, null, null));
        assertDoesNotThrow(() -> repository.record("employee", "UPDATE", "505", 505, null, null));
    }
}
