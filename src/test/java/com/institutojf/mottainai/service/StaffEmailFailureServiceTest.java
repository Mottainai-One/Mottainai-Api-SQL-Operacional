package com.institutojf.mottainai.service;

import com.institutojf.mottainai.repository.EmployeeInvitationTokenRepository;
import com.institutojf.mottainai.repository.PasswordResetTokenRepository;
import com.institutojf.mottainai.model.enums.PasswordTokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffEmailFailureServiceTest {

    @Mock
    private RlsContextService rlsContextService;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmployeeInvitationTokenRepository invitationTokenRepository;

    @InjectMocks
    private StaffEmailFailureService service;

    @Test
    @DisplayName("Should invalidates undelivered invitation")
    void invalidatesUndeliveredInvitation() {
        Long invitationId = 17L;
        when(rlsContextService.bootstrapByToken("token-hash", PasswordTokenType.EMPLOYEE_INVITATION.name()))
            .thenReturn(2);
        when(invitationTokenRepository.findUnusedByHashForUpdate("token-hash")).thenReturn(Optional
            .of(new EmployeeInvitationTokenRepository.Invitation(invitationId, 2, OffsetDateTime.now().plusHours(1))));
        service.invalidateInvitationToken("token-hash");
        verify(invitationTokenRepository).markUsed(invitationId);
    }

}
