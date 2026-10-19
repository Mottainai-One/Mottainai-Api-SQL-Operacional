package com.institutojf.mottainai.service;

import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.mapper.RetailStoreMapper;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private RetailStoreMapper retailStoreMapper;
    @Mock private EmployeePrivateDataService privateDataService;

    @Test
    void returnsEmployeeContactFromPrivateDataUsingFirebaseUid() {
        String uid = "firebase-uid-42";
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        Employee employee = new Employee();
        employee.setId(12);
        employee.setRole(role);
        employee.setActive(true);
        RetailStore store = new RetailStore();
        store.setId(5);
        employee.setStore(store);
        AppUser user = new AppUser();
        user.setId(7);
        user.setFirebaseUid(uid);
        user.setActive(true);
        user.setEmployee(employee);
        when(appUserRepository.findByFirebaseUidAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(privateDataService.name(employee)).thenReturn("Ana Silva");
        when(privateDataService.cpfMasked(employee)).thenReturn("***.***.***-**");
        when(privateDataService.email(employee)).thenReturn("ana@example.com");
        when(privateDataService.phone(employee)).thenReturn("***-**42");
        UserProfileService service = new UserProfileService(appUserRepository, retailStoreMapper, privateDataService);

        var response = service.me(uid);

        assertEquals("Ana Silva", response.name());
        assertEquals("***.***.***-**", response.cpf());
        assertEquals("ana@example.com", response.email());
        assertEquals("***-**42", response.phone());
        assertEquals("MANAGER", response.role());
        verify(appUserRepository).findByFirebaseUidAndDeletedAtIsNull(uid);
    }

    @Test
    void doesNotResolveProfileByEmail() {
        when(appUserRepository.findByFirebaseUidAndDeletedAtIsNull("email@example.com"))
                .thenReturn(Optional.empty());
        UserProfileService service = new UserProfileService(appUserRepository, retailStoreMapper, privateDataService);

        assertThrows(ResourceNotFoundException.class, () -> service.me("email@example.com"));
    }

    @Test
    void refusesProfileForInactiveStaffAccount() {
        String uid = "inactive-firebase-uid";
        AppUser user = profileUser(uid, false);
        when(appUserRepository.findByFirebaseUidAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        UserProfileService service = new UserProfileService(appUserRepository, retailStoreMapper, privateDataService);

        assertThrows(ResourceNotFoundException.class, () -> service.me(uid));
    }

    @Test
    void returnsStoreForAuthenticatedUid() {
        String uid = "firebase-uid-42";
        AppUser user = profileUser(uid, true);
        RetailStoreResponse expected = new RetailStoreResponse(5, null, null, "Store", "12345678901234",
                null, null, null, null, true);
        when(appUserRepository.findByFirebaseUidAndDeletedAtIsNull(uid)).thenReturn(Optional.of(user));
        when(retailStoreMapper.toResponse(user.getEmployee().getStore())).thenReturn(expected);
        UserProfileService service = new UserProfileService(appUserRepository, retailStoreMapper, privateDataService);

        assertEquals(expected, service.myStore(uid));
        verify(retailStoreMapper).toResponse(user.getEmployee().getStore());
    }

    @Test
    void listsAndFindsUsersWithEncryptedStaffContactProjection() {
        AppUser user = profileUser("firebase-uid-42", true);
        when(appUserRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(user));
        when(appUserRepository.findById(7)).thenReturn(Optional.of(user));
        when(privateDataService.name(user.getEmployee())).thenReturn("Ana Silva");
        when(privateDataService.cpfMasked(user.getEmployee())).thenReturn("***.***.***-**");
        when(privateDataService.email(user.getEmployee())).thenReturn("ana@example.com");
        when(privateDataService.phone(user.getEmployee())).thenReturn("***-**42");
        UserProfileService service = new UserProfileService(appUserRepository, retailStoreMapper, privateDataService);

        var listed = service.findAll();
        var found = service.findById(7);

        assertEquals(1, listed.size());
        assertEquals("Ana Silva", listed.get(0).name());
        assertEquals("ana@example.com", found.email());
        assertEquals("***.***.***-**", found.cpf());
    }

    private static AppUser profileUser(String uid, boolean active) {
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        Employee employee = new Employee();
        employee.setId(12);
        employee.setRole(role);
        employee.setActive(active);
        RetailStore store = new RetailStore();
        store.setId(5);
        employee.setStore(store);
        AppUser user = new AppUser();
        user.setId(7);
        user.setFirebaseUid(uid);
        user.setActive(active);
        user.setEmployee(employee);
        return user;
    }
}
