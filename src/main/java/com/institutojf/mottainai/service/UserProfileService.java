package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.dto.response.UserResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.RetailStoreMapper;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository appUserRepository;

    private final RetailStoreMapper retailStoreMapper;

    private final EmployeePrivateDataService privateDataService;

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        return toOwnProfileResponse(findUser(email));
    }

    @Transactional(readOnly = true)
    public RetailStoreResponse myStore(String email) {
        return retailStoreMapper.toResponse(findUser(email).getEmployee().getStore());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return appUserRepository.findAllByDeletedAtIsNull().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        return toResponse(appUserRepository.findById(id)
            .filter(user -> user.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private AppUser findUser(String firebaseUid) {
        return appUserRepository.findByFirebaseUidAndDeletedAtIsNull(firebaseUid)
            .filter(user -> Boolean.TRUE.equals(user.getActive())
                    && Boolean.TRUE.equals(user.getEmployee().getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(AppUser user) {
        Employee employee = user.getEmployee();
        return new UserResponse(user.getId(), privateDataService.name(employee),
                privateDataService.cpfMasked(employee), privateDataService.email(employee),
                privateDataService.phone(employee), employee.getRole().getName(),
                user.getActive(), employee.getStore().getId());
    }

    private UserResponse toOwnProfileResponse(AppUser user) {
        Employee employee = user.getEmployee();
        return new UserResponse(user.getId(), privateDataService.name(employee),
                privateDataService.cpf(employee), privateDataService.email(employee),
                privateDataService.phone(employee), employee.getRole().getName(),
                user.getActive(), employee.getStore().getId());
    }
}
