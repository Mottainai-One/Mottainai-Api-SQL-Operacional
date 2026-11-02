package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.ReplenishmentPreList;
import com.institutojf.mottainai.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReplenishmentPreListResponse(
        Integer id,
        Integer storeId,
        Integer employeeId,
        LocalDateTime generatedAt,
        PreListStatus status,
        List<Item> items
) {

    public static ReplenishmentPreListResponse from(ReplenishmentPreList list) {
        return new ReplenishmentPreListResponse(list.getId(), list.getStore().getId(),
                list.getEmployee() == null ? null : list.getEmployee().getId(), list.getGeneratedAt(), list.getStatus(),
                list.getItems()
                    .stream()
                    .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
                    .map(i -> new Item(i.getId(), i.getProduct().getId(), i.getSuggestedQuantity(), i.getPriority()))
                    .toList());
    }

    public record Item(
            Integer id,
            Integer productId,
            BigDecimal suggestedQuantity,
            PriorityLevel priority
    ) {
    }
}
