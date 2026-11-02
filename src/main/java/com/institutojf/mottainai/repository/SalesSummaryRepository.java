package com.institutojf.mottainai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class SalesSummaryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SummaryTotals summarizeCurrentMonth(Integer storeId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT COUNT(*) AS transaction_count,
                       COALESCE(SUM(total_amount), 0) AS total_amount
                  FROM mottainai.sales_transaction
                 WHERE store_id = :storeId
                   AND sale_date >= :from
                   AND sale_date < :to
                   AND status = 'COMPLETED'
                   AND deleted_at IS NULL
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("storeId", storeId)
                .addValue("from", from)
                .addValue("to", to);
        return jdbcTemplate.queryForObject(sql, parameters,
                (resultSet, rowNumber) -> new SummaryTotals(
                        resultSet.getLong("transaction_count"),
                        resultSet.getBigDecimal("total_amount")));
    }

    public record SummaryTotals(long transactionCount, BigDecimal totalAmount) {
    }
}
