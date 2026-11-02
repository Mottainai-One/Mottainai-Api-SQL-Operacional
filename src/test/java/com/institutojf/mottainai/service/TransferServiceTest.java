package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.Transfer;
import com.institutojf.mottainai.model.enums.TransferStatus;
import com.institutojf.mottainai.repository.BatchRepository;
import com.institutojf.mottainai.repository.InventoryRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.repository.SuggestedActionRepository;
import com.institutojf.mottainai.repository.TransferRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private SuggestedActionRepository suggestedActionRepository;

    @Mock
    private InventoryMovementService inventoryMovementService;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferService service;

    @Test
    void returnsTheCompletedTransferWhenTheReceiptKeyIsReplayed() {
        Transfer transfer = transfer();
        when(outboxEventRepository.findByIdempotencyKey("receipt-key"))
            .thenReturn(Optional.of(new OutboxEventRepository.Event("TRANSFER_RECEIVED", "transfer", "5", "{}")));
        when(outboxEventRepository.isEventFor(any(), eq("TRANSFER_RECEIVED"), eq("transfer"))).thenReturn(true);
        when(transferRepository.findById(5)).thenReturn(Optional.of(transfer));
        when(inventoryAccess.resolveStoreId(authentication, 1)).thenReturn(1);
        var response = service.receive(5, "receipt-key", authentication);
        assertEquals(TransferStatus.COMPLETED, response.status());
        verify(inventoryMovementService, never()).create(anyInt(), any(), any());
    }

    private Transfer transfer() {
        RetailStore source = new RetailStore();
        source.setId(1);
        RetailStore destination = new RetailStore();
        destination.setId(2);
        Employee employee = new Employee();
        employee.setId(3);
        Transfer transfer = new Transfer();
        transfer.setId(5);
        transfer.setSourceStore(source);
        transfer.setDestinationStore(destination);
        transfer.setEmployee(employee);
        transfer.setStatus(TransferStatus.COMPLETED);
        return transfer;
    }

}
