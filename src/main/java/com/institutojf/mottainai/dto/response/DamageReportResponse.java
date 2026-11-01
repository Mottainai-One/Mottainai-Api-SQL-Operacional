package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DamageReportResponse(Long id, Integer storeId, Integer employeeId, Integer productId,
        String reason, BigDecimal quantity, String note, String photoUrl, OffsetDateTime createdAt) { }
