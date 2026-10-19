package com.institutojf.mottainai.service;

import java.util.Optional;
import org.springframework.stereotype.Component;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;

/** Resolves a staff account by the verified Firebase UID, never by PII. */
@Component
@RequiredArgsConstructor
public class StaffUserLookup {

    private final AppUserRepository appUserRepository;

    public Optional<AppUser> findActive(String firebaseUid) {
        return appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid);
    }

    public Optional<AppUser> findAny(String firebaseUid) {
        return appUserRepository.findByFirebaseUidAndDeletedAtIsNull(firebaseUid);
    }

    public Optional<AppUser> findActiveForUpdate(String firebaseUid) {
        return appUserRepository.findActiveByFirebaseUidWithWriteLock(firebaseUid);
    }

    public boolean exists(String firebaseUid) {
        return appUserRepository.existsByFirebaseUid(firebaseUid);
    }
}
