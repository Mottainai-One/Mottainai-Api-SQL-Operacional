package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.MobileProfileResponse;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.security.StaffRoleAuthorities;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MobileProfileService {
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public MobileProfileResponse me(String firebaseUid) {
        AppUser user = appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("Operational account is unavailable"));
        var employee = user.getEmployee();
        if (!firebaseUid.equals(user.getFirebaseUid()) || !Boolean.TRUE.equals(user.getActive())
                || user.getDeletedAt() != null || employee == null || !Boolean.TRUE.equals(employee.getActive())
                || employee.getDeletedAt() != null || employee.getRole() == null
                || !Boolean.TRUE.equals(employee.getRole().getActive()) || employee.getRole().getDeletedAt() != null
                || employee.getStore() == null || !Boolean.TRUE.equals(employee.getStore().getActive())
                || employee.getStore().getDeletedAt() != null || employee.getStore().getCompany() == null
                || !Boolean.TRUE.equals(employee.getStore().getCompany().getActive())
                || employee.getStore().getCompany().getDeletedAt() != null) {
            throw new AccessDeniedException("Operational account is unavailable");
        }
        Integer level = employee.getRole().getPermissionLevel();
        String role = StaffRoleAuthorities.mobileRoleForPermissionLevel(level);
        if (role == null) throw new AccessDeniedException("Employee role is not supported by the mobile app");
        // Identity display comes from Firebase. Authorization never needs encrypted PII.
        return new MobileProfileResponse(user.getId(), user.getFirebaseUid(), null,
                null, role, true, employee.getStore().getId(),
                employee.getStore().getCompany().getId(), StaffRoleAuthorities.forPermissionLevel(level));
    }
}
