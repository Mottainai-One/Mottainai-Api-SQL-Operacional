package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeShiftResponse(
        Integer shiftId,
        Integer terminalId,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingAmount,
        BigDecimal closingAmount,
        String status
) {
}
