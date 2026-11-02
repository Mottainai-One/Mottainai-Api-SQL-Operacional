package com.institutojf.mottainai.security;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.service.RlsContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    private final RlsContextService rlsContextService;

    // Valida o Firebase UID recebido somente após a validação do ID token Firebase
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank() || !rlsContextService.bootstrapByFirebaseUid(firebaseUid)) {
            throw new UsernameNotFoundException("Invalid Firebase identity or password");
        }
        AppUser user = appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid)
            .filter(this::hasActiveEmployment)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid Firebase identity or password"));
        String role = user.getEmployee().getRole().getName().toUpperCase(Locale.ROOT);
        return User.withUsername(user.getFirebaseUid()).password(user.getPasswordHash()).authorities("ROLE_" + role).build();
    }

    private boolean hasActiveEmployment(AppUser user) {
        Employee employee = user.getEmployee();
        EmployeeRole role = employee.getRole();
        return Boolean.TRUE.equals(employee.getActive()) && employee.getDeletedAt() == null
                && Boolean.TRUE.equals(role.getActive()) && role.getDeletedAt() == null;
    }

}
