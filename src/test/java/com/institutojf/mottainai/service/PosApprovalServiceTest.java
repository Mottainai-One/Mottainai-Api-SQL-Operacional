package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.DecidePosApprovalRequest;
import com.institutojf.mottainai.dto.response.PosApprovalResponse;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.PosApprovalStorageUnavailableException;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.PosApprovalRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PosApprovalServiceTest {
    private final PosApprovalRepository repository = mock(PosApprovalRepository.class);
    private final InventoryAccess access = mock(InventoryAccess.class);
    private final AuditLogRepository audit = mock(AuditLogRepository.class);
    private final PosApprovalService service = new PosApprovalService(repository, access, audit);
    private final TestingAuthenticationToken manager = new TestingAuthenticationToken("bound-uid", null, "ROLE_MANAGER");
    private final UUID id = UUID.randomUUID();
    private AppUser actor;

    @BeforeEach void setUp() {
        RetailStore store = new RetailStore(); store.setId(4);
        Employee employee = new Employee(); employee.setId(505); employee.setStore(store);
        actor = new AppUser(); actor.setId(91); actor.setEmployee(employee); actor.setFirebaseUid("bound-uid");
    }

    @Test void refusesOperatorBeforeReadingQueue() {
        assertThrows(AccessDeniedException.class, () -> service.list("PENDING",50,
                new TestingAuthenticationToken("operator", null, "ROLE_OPERATOR")));
        verifyNoInteractions(repository, access, audit);
    }

    @Test void scopesQueueToCurrentDatabaseAssignment() {
        when(access.currentUser(manager)).thenReturn(actor);
        when(repository.list(4,"PENDING",50)).thenReturn(List.of(row("SPECIAL_DISCOUNT","PENDING")));
        assertEquals(4,service.list("PENDING",50,manager).getFirst().storeId());
        verify(repository).list(4,"PENDING",50);
    }

    @Test void refusesAlreadyDecidedRequestWithoutExecutingItAgain() {
        when(access.currentUser(manager)).thenReturn(actor);
        when(repository.lock(id,4)).thenReturn(Optional.of(row("CANCEL_PRODUCT","EXECUTED")));
        assertThrows(ConflictException.class, () -> service.decide(id,new DecidePosApprovalRequest("APPROVED",null),manager));
        verify(repository,never()).decideCancellation(any(),any(),anyBoolean(),any());
        verifyNoInteractions(audit);
    }

    @Test void refusesSaleFromAnotherStoreBeforeChangingAnything() {
        when(access.currentUser(manager)).thenReturn(actor);
        when(repository.lock(id,4)).thenReturn(Optional.of(row("CANCEL_PRODUCT","PENDING")));
        assertThrows(ConflictException.class, () -> service.decide(id,new DecidePosApprovalRequest("APPROVED",null),manager));
        verify(repository,never()).saveDecision(any(),any(),any(),any(),any(),any());
        verifyNoInteractions(audit);
    }

    @Test void executesMatchingCancellationAndAuditsAuthenticatedUserNotRequester() {
        when(access.currentUser(manager)).thenReturn(actor);
        var pending = row("CANCEL_PRODUCT","PENDING"); var executed = row("CANCEL_PRODUCT","EXECUTED");
        when(repository.lock(id,4)).thenReturn(Optional.of(pending));
        when(repository.validSaleStore(pending)).thenReturn(true);
        when(repository.validPendingCancellation(pending)).thenReturn(true);
        when(repository.saveDecision(id,4,"EXECUTED",505,"bound-uid","Conferido")).thenReturn(executed);
        assertSame(executed,service.decide(id,new DecidePosApprovalRequest("APPROVED","Conferido"),manager));
        var sequence = inOrder(repository,audit);
        sequence.verify(repository).decideCancellation(18,505,true,"Conferido");
        sequence.verify(repository).saveDecision(id,4,"EXECUTED",505,"bound-uid","Conferido");
        sequence.verify(audit).record(eq("pos_manager_approval_request"),eq("UPDATE"),eq(id.toString()),eq(91),any(),any());
    }

    @Test void missingDatabaseContractIsReportedWithoutSyntheticSuccess() {
        when(access.currentUser(manager)).thenReturn(actor);
        when(repository.list(4,"PENDING",50)).thenThrow(new UncategorizedSQLException("queue","SELECT",
                new SQLException("Missing table","42P01")));
        assertThrows(PosApprovalStorageUnavailableException.class, () -> service.list("PENDING",50,manager));
    }

    private PosApprovalResponse row(String type,String status) {
        return new PosApprovalResponse(id,4,17,20,LocalDateTime.of(2026,11,2,10,0),3,18,type,
                "Solicitação do PDV",new BigDecimal("12.50"),null,status,null,OffsetDateTime.now(),null);
    }
}
