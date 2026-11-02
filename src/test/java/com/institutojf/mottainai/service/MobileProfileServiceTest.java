package com.institutojf.mottainai.service;

import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileProfileServiceTest {
    @Mock AppUserRepository users;
    @Mock EmployeePrivateDataService privateData;

    @ParameterizedTest
    @CsvSource({"100,dono", "80,gerente", "60,estoquista", "40,estoquista"})
    void derivesRoleAndTenantFromDatabaseInsteadOfRoleDisplayNames(int level, String expectedRole) {
        AppUser user = account(level);
        when(users.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("verified-uid"))
                .thenReturn(Optional.of(user));
        var result = new MobileProfileService(users).me("verified-uid");
        assertEquals("verified-uid", result.firebaseUid());
        assertEquals(expectedRole, result.role());
        assertEquals(5, result.storeId());
        assertEquals(3, result.companyId());
        assertNull(result.name());
        assertNull(result.email());
        verifyNoInteractions(privateData);
        assertFalse(result.permissions().isEmpty());
    }

    @Test
    void refusesUnknownUidWithoutCreatingOrLinkingAccount() {
        when(users.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("unknown"))
                .thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> new MobileProfileService(users).me("unknown"));
        verifyNoInteractions(privateData);
        verify(users, never()).save(any());
    }

    @Test
    void refusesInactiveOrDeletedAssignmentAtEveryLevel() {
        for (int field = 0; field < 10; field++) {
            AppUser user = account(80);
            switch (field) {
                case 0 -> user.setActive(false);
                case 1 -> user.setDeletedAt(LocalDateTime.now());
                case 2 -> user.getEmployee().setActive(false);
                case 3 -> user.getEmployee().setDeletedAt(LocalDateTime.now());
                case 4 -> user.getEmployee().getRole().setActive(false);
                case 5 -> user.getEmployee().getRole().setDeletedAt(LocalDateTime.now());
                case 6 -> user.getEmployee().getStore().setActive(false);
                case 7 -> user.getEmployee().getStore().setDeletedAt(LocalDateTime.now());
                case 8 -> user.getEmployee().getStore().getCompany().setActive(false);
                case 9 -> user.getEmployee().getStore().getCompany().setDeletedAt(LocalDateTime.now());
            }
            when(users.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("verified-uid"))
                    .thenReturn(Optional.of(user));
            assertThrows(AccessDeniedException.class,
                    () -> new MobileProfileService(users).me("verified-uid"));
        }
        verifyNoInteractions(privateData);
    }

    @Test
    void refusesUnsupportedRoleAndWrongDatabaseUid() {
        AppUser user = account(20);
        when(users.findByFirebaseUidAndActiveTrueAndDeletedAtIsNull("verified-uid"))
                .thenReturn(Optional.of(user));
        assertThrows(AccessDeniedException.class,
                () -> new MobileProfileService(users).me("verified-uid"));
        user.getEmployee().getRole().setPermissionLevel(100);
        user.setFirebaseUid("another-uid");
        assertThrows(AccessDeniedException.class,
                () -> new MobileProfileService(users).me("verified-uid"));
        verifyNoInteractions(privateData);
    }

    private AppUser account(int level) {
        Company company = new Company(); company.setId(3); company.setActive(true);
        RetailStore store = new RetailStore(); store.setId(5); store.setActive(true); store.setCompany(company);
        EmployeeRole role = new EmployeeRole(); role.setName("Custom display label");
        role.setPermissionLevel(level); role.setActive(true);
        Employee employee = new Employee(); employee.setId(12); employee.setActive(true);
        employee.setStore(store); employee.setRole(role);
        AppUser user = new AppUser(); user.setId(7); user.setFirebaseUid("verified-uid");
        user.setActive(true); user.setEmployee(employee);
        return user;
    }
}
