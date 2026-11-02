package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Integer id,
        Integer storeId,
        Integer supplierId,
        Integer employeeId,
        LocalDateTime orderDate,
        LocalDate expectedDeliveryDate,
        PurchaseOrderStatus status,
        String observation,
        BigDecimal totalAmount,
        Integer version,
        List<PurchaseOrderItemResponse> items
) {
}
