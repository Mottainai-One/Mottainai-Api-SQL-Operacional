package com.institutojf.mottainai.security;

import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.service.RlsContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RlsContextService rlsContextService;

    @Test
    void resolvesStaffAccountByFirebaseUid() {
        String firebaseUid = "firebase-user-123";
        EmployeeRole role = new EmployeeRole();
        role.setName("OPERATOR");
        role.setActive(true);
        Employee employee = new Employee();
        employee.setActive(true);
        employee.setRole(role);
        AppUser user = new AppUser();
        user.setFirebaseUid(firebaseUid);
        user.setActive(true);
        user.setPasswordHash("encoded-password");
        user.setEmployee(employee);

        when(rlsContextService.bootstrapByFirebaseUid(firebaseUid)).thenReturn(true);
        when(appUserRepository.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid))
                .thenReturn(Optional.of(user));

        var details = new DatabaseUserDetailsService(appUserRepository, rlsContextService)
                .loadUserByUsername(firebaseUid);

        assertEquals(firebaseUid, details.getUsername());
        assertEquals("encoded-password", details.getPassword());
        assertEquals("ROLE_OPERATOR", details.getAuthorities().iterator().next().getAuthority());
        verify(rlsContextService).bootstrapByFirebaseUid(firebaseUid);
        verify(appUserRepository).findByFirebaseUidAndActiveTrueAndDeletedAtIsNull(firebaseUid);
    }

    @Test
    void rejectsUidWhenDatabaseBootstrapDoesNotResolveIt() {
        when(rlsContextService.bootstrapByFirebaseUid("unlinked-uid")).thenReturn(false);

        assertThrows(UsernameNotFoundException.class,
                () -> new DatabaseUserDetailsService(appUserRepository, rlsContextService)
                        .loadUserByUsername("unlinked-uid"));
    }
}
