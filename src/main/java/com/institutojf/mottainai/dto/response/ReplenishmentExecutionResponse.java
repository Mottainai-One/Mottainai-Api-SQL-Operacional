package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.ReplenishmentExecution;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReplenishmentExecutionResponse(
        Integer id,
        Integer preListId,
        Integer employeeId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer rating,
        String comment,
        List<Item> items
) {

    public static ReplenishmentExecutionResponse from(ReplenishmentExecution execution) {
        return new ReplenishmentExecutionResponse(
                execution.getId(),
                execution.getPreList().getId(),
                execution.getEmployee().getId(),
                execution.getStartDate(),
                execution.getEndDate(),
                execution.getRating(),
                execution.getComment(),
                execution.getItems()
                    .stream()
                    .map(i -> new Item(i.getId(), i.getBatch().getId(), i.getReplenishedQuantity()))
                    .toList());
    }
    public record Item(
            Integer id,
            Integer batchId,
            @Schema(name = "replenishedQuantity", description = "Quantity replenished for this batch", example = "3")
            BigDecimal replenishedQuantity
    ) {
    }

}
