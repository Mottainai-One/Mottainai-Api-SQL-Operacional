package com.institutojf.mottainai.service;

import com.institutojf.mottainai.repository.PasswordResetTokenRepository;
import com.institutojf.mottainai.repository.EmployeeInvitationTokenRepository;
import com.institutojf.mottainai.model.enums.PasswordTokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class StaffEmailFailureService {

    private final RlsContextService rlsContextService;

    private final PasswordResetTokenRepository tokenRepository;

    private final EmployeeInvitationTokenRepository invitationTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateRecoveryToken(String hash) {
        Integer userId = rlsContextService.bootstrapByToken(hash, PasswordTokenType.PASSWORD_RESET.name());
        if (userId != null) {
            tokenRepository.findUnusedByHashForUpdate(hash, PasswordTokenType.PASSWORD_RESET).ifPresent(token -> {
                token.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
                tokenRepository.save(token);
            });
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateInvitationToken(String hash) {
        Integer userId = rlsContextService.bootstrapByToken(hash, PasswordTokenType.EMPLOYEE_INVITATION.name());
        if (userId != null) {
            invitationTokenRepository.findUnusedByHashForUpdate(hash)
                .ifPresent(token -> invitationTokenRepository.markUsed(token.id()));
        }
    }

}
