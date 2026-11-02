package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.dto.response.EmployeeCancelRequestResponse;
import com.institutojf.mottainai.dto.response.EmployeeShiftResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmployeeActivityRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<EmployeeShiftResponse> findShifts(Integer employeeId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
                SELECT shift_id, terminal_id, opened_at, closed_at,
                       opening_amount, closing_amount, status
                  FROM mottainai.pos_shift
                 WHERE employee_id = ? AND opened_at >= ? AND opened_at <= ?
                 ORDER BY opened_at DESC, shift_id DESC
                """,
                (resultSet, rowNumber) -> new EmployeeShiftResponse(resultSet.getInt("shift_id"),
                        resultSet.getInt("terminal_id"), resultSet.getTimestamp("opened_at").toLocalDateTime(),
                        resultSet.getTimestamp("closed_at") == null ? null
                                : resultSet.getTimestamp("closed_at").toLocalDateTime(),
                        resultSet.getBigDecimal("opening_amount"), resultSet.getBigDecimal("closing_amount"),
                        resultSet.getString("status")),
                employeeId, from, to);
    }

    public List<EmployeeCancelRequestResponse> findCancelRequests(Integer employeeId, LocalDateTime from,
            LocalDateTime to) {
        return jdbcTemplate.query("""
                SELECT cancel_request_id, sale_id, sale_item_id,
                       target_type::TEXT AS target_type, reason,
                       status::TEXT AS status, requested_at, decided_at
                  FROM mottainai.pos_cancel_request
                 WHERE requested_by = ? AND requested_at >= ? AND requested_at <= ?
                 ORDER BY requested_at DESC, cancel_request_id DESC
                """,
                (resultSet,
                        rowNumber) -> new EmployeeCancelRequestResponse(resultSet.getInt("cancel_request_id"),
                                resultSet.getInt("sale_id"), resultSet.getObject("sale_item_id", Integer.class),
                                resultSet.getString("target_type"), resultSet.getString("reason"),
                                resultSet.getString("status"), resultSet.getTimestamp("requested_at").toLocalDateTime(),
                                resultSet.getTimestamp("decided_at") == null ? null
                                        : resultSet.getTimestamp("decided_at").toLocalDateTime()),
                employeeId, from, to);
    }

}
