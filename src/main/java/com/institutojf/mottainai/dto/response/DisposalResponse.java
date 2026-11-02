package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Disposal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DisposalResponse(
        Integer id,
        Integer storeId,
        Integer employeeId,
        String reason,
        LocalDateTime disposalDate,
        String observation,
        Integer version,
        List<Item> items
) {

    public static DisposalResponse from(Disposal disposal) {
        return new DisposalResponse(disposal.getId(),
                disposal.getStore().getId(),
                disposal.getEmployee().getId(),
                disposal.getReason(),
                disposal.getDisposalDate(),
                disposal.getObservation(),
                disposal.getVersion(),
                disposal.getItems()
                    .stream()
                    .map(i -> new Item(i.getId(), i.getBatch().getId(), i.getDisposedQuantity()))
                    .toList());
    }
    public record Item(
            Integer id,
            Integer batchId,
            BigDecimal disposedQuantity
    ) {
    }
}
