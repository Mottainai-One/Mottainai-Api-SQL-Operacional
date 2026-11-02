package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Transfer;
import com.institutojf.mottainai.model.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransferResponse(
        Integer id,
        Integer sourceStoreId,
        Integer destinationStoreId,
        Integer employeeId,
        TransferStatus status,
        String observation,
        LocalDateTime requestDate,
        LocalDateTime completionDate,
        Integer version,
        List<Item> items
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getSourceStore().getId(),
                transfer.getDestinationStore().getId(), transfer.getEmployee().getId(), transfer.getStatus(),
                transfer.getObservation(), transfer.getRequestDate(), transfer.getCompletionDate(),
                transfer.getVersion(),
                transfer.getItems()
                    .stream()
                    .map(item -> new Item(item.getBatch().getId(), item.getTransferredQuantity()))
                    .toList());
    }
    public record Item(
            Integer batchId,
            BigDecimal transferredQuantity
    ) {
    }
}
