package com.institutojf.mottainai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SupplierPurchaseHistoryResponse(
        Integer purchaseOrderId,
        Integer storeId,
        Integer employeeId,
        LocalDateTime orderDate,
        LocalDate expectedDeliveryDate,
        String status,
        BigDecimal totalAmount
) {
}
