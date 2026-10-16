package com.institutojf.mottainai.security;

import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class InventoryAccessTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private Authentication authentication;

    @Test
    @DisplayName("Should derive manager store instead of using requested store")
    void shouldDeriveManagerStoreInsteadOfUsingRequestedStore() {
        InventoryAccess access = new InventoryAccess(appUserRepository, retailStoreRepository);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("manager@example.com"))
            .thenReturn(Optional.of(userInStoreWithRole(2, "MANAGER")));
        Integer storeId = access.resolveStoreId(authentication, 99);
        assertEquals(2, storeId);
    }

    @Test
    @DisplayName("Should reject manager access to another store")
    void shouldRejectManagerAccessToAnotherStore() {
        InventoryAccess access = new InventoryAccess(appUserRepository, retailStoreRepository);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("manager@example.com");
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("manager@example.com"))
            .thenReturn(Optional.of(userInStoreWithRole(2, "MANAGER")));
        assertThrows(BusinessException.class, () -> access.checkStoreAccess(authentication, 99));
    }

    @Test
    @DisplayName("Should require store parameter for administrator list")
    void shouldRequireStoreParameterForAdministratorList() {
        InventoryAccess access = new InventoryAccess(appUserRepository, retailStoreRepository);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin@example.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))).when(authentication).getAuthorities();
        when(appUserRepository.findByEmailIgnoreCaseAndActiveTrueAndDeletedAtIsNull("admin@example.com"))
            .thenReturn(Optional.of(userInStoreWithRole(2, "ADMINISTRATOR")));
        assertThrows(BusinessException.class, () -> access.resolveStoreId(authentication, null));
    }

    private AppUser userInStoreWithRole(Integer storeId, String roleName) {
        RetailStore store = new RetailStore();
        store.setId(storeId);
        EmployeeRole role = new EmployeeRole();
        role.setName(roleName);
        Employee employee = new Employee();
        employee.setStore(store);
        employee.setRole(role);
        AppUser user = new AppUser();
        user.setEmployee(employee);
        return user;
    }

}
