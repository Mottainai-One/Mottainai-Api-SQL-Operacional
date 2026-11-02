package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.dto.response.PosApprovalResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PosApprovalRepository {
    private static final String COLUMNS = """
            request_id, store_id, employee_id, sale_id, sale_date, sale_item_id,
            cancellation_request_id, request_type, reason, requested_amount, discount_percent,
            status, decided_by_employee_id, created_at, decided_at
            """;
    private static final RowMapper<PosApprovalResponse> ROW = (rs, row) -> new PosApprovalResponse(
            rs.getObject("request_id", UUID.class), rs.getObject("store_id", Integer.class),
            rs.getObject("employee_id", Integer.class), rs.getObject("sale_id", Integer.class),
            rs.getObject("sale_date", LocalDateTime.class), rs.getObject("sale_item_id", Integer.class),
            rs.getObject("cancellation_request_id", Integer.class), rs.getString("request_type"),
            rs.getString("reason"), rs.getBigDecimal("requested_amount"), rs.getBigDecimal("discount_percent"),
            rs.getString("status"), rs.getObject("decided_by_employee_id", Integer.class),
            rs.getObject("created_at", OffsetDateTime.class), rs.getObject("decided_at", OffsetDateTime.class));
    private final JdbcTemplate jdbc;

    public List<PosApprovalResponse> list(Integer storeId, String status, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM mottainai.pos_manager_approval_request "
                + "WHERE store_id=? AND status=? ORDER BY created_at DESC, request_id LIMIT ?", ROW, storeId, status, limit);
    }

    public Optional<PosApprovalResponse> lock(UUID id, Integer storeId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM mottainai.pos_manager_approval_request "
                + "WHERE request_id=? AND store_id=? FOR UPDATE", ROW, id, storeId).stream().findFirst();
    }

    public boolean validSaleStore(PosApprovalResponse request) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM mottainai.sales_transaction
                  WHERE sale_id=? AND sale_date=? AND store_id=?)
                """, Boolean.class, request.saleId(), request.saleDate(), request.storeId()));
    }

    public boolean validPendingCancellation(PosApprovalResponse request) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM mottainai.pos_cancel_request
                  WHERE cancel_request_id=? AND sale_id=? AND sale_date=?
                  AND sale_item_id IS NOT DISTINCT FROM CAST(? AS INTEGER) AND status='PENDING')
                """, Boolean.class, request.cancellationRequestId(), request.saleId(), request.saleDate(), request.saleItemId()));
    }

    public void decideCancellation(Integer cancellationId, Integer managerEmployeeId, boolean approve, String observation) {
        jdbc.update("CALL mottainai.sp_decide_pos_cancellation(?, ?, ?, ?)",
                cancellationId, managerEmployeeId, approve, observation);
    }

    public PosApprovalResponse saveDecision(UUID id, Integer storeId, String status, Integer employeeId,
            String firebaseUid, String observation) {
        return jdbc.queryForObject("""
                UPDATE mottainai.pos_manager_approval_request SET status=?, decided_by_employee_id=?,
                  decided_by_firebase_uid=?, decision_observation=?, decided_at=CURRENT_TIMESTAMP,
                  updated_at=CURRENT_TIMESTAMP WHERE request_id=? AND store_id=? AND status='PENDING'
                RETURNING
                """ + COLUMNS, ROW, status, employeeId, firebaseUid, observation, id, storeId);
    }
}
