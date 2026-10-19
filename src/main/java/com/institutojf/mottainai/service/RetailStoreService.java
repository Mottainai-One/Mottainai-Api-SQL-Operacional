package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.UpdateRetailStoreRequest;
import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.RetailStoreMapper;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.CompanyRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RetailStoreService {

    private final RetailStoreRepository retailStoreRepository;

    private final CompanyRepository companyRepository;

    private final AddressService addressService;

    private final AppUserRepository appUserRepository;

    private final StaffUserLookup staffUserLookup;

    private final AuditLogRepository auditLogRepository;

    private final RetailStoreMapper retailStoreMapper;

    @Transactional
    public RetailStoreResponse create(CreateRetailStoreRequest request) {
        if (retailStoreRepository.existsByCnpj(request.cnpj())) {
            throw new ConflictException("Store CNPJ already exists");
        }
        Company company = findActiveCompanyById(request.companyId());
        ensureStoreLimitNotReached(company);
        RetailStore store = new RetailStore();
        store.setCompany(company);
        store.setCnpj(request.cnpj());
        store.setActive(true);
        store.setAddress(addressService.createAddress(request.address()));
        applyStoreFields(request.name(), request.email(), request.phone(), request.latitude(), request.longitude(), store);
        return retailStoreMapper.toResponse(retailStoreRepository.save(store));
    }

    @Transactional(readOnly = true)
    public Page<RetailStoreResponse> findAll(Pageable pageable) {
        return retailStoreRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable).map(retailStoreMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RetailStoreResponse findById(Integer id) {
        return retailStoreMapper.toResponse(findActiveStoreById(id));
    }

    @Transactional
    public RetailStoreResponse update(Integer id, UpdateRetailStoreRequest request) {
        RetailStore store = findStoreById(id);
        // Reativar uma loja também consome uma vaga do plano contratado
        if (Boolean.TRUE.equals(request.active()) && Boolean.FALSE.equals(store.getActive())) {
            ensureStoreLimitNotReached(store.getCompany());
        }
        addressService.updateAddress(store.getAddress().getId(), request.address());
        applyStoreFields(request.name(), request.email(), request.phone(), request.latitude(), request.longitude(), store);
        store.setActive(request.active());
        return retailStoreMapper.toResponse(retailStoreRepository.save(store));
    }

    @Transactional
    public RetailStoreResponse updateStatus(Integer id, Boolean active) {
        RetailStore store = findStoreById(id);
        if (Boolean.TRUE.equals(active) && Boolean.FALSE.equals(store.getActive())) {
            ensureStoreLimitNotReached(store.getCompany());
        }
        store.setActive(active);
        return retailStoreMapper.toResponse(retailStoreRepository.save(store));
    }

    @Transactional
    public void delete(Integer id, String actorEmail) {
        AppUser actor = staffUserLookup.findActive(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        RetailStore store = findActiveStoreById(id);
        store.setActive(false);
        store.setDeletedAt(LocalDateTime.now());
        retailStoreRepository.save(store);
        auditLogRepository.record("retail_store", "DELETE", id.toString(), actor.getId(), Map.of("active", true),
                Map.of("active", false, "deleted_at", store.getDeletedAt().toString()));
    }

    /**
     * Garante que a empresa não ultrapasse o número de lojas contratado no plano de
     * assinatura
     */
    private void ensureStoreLimitNotReached(Company company) {
        long activeStores = retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(company.getId());
        if (activeStores >= company.getPlan().getStoreLimit()) {
            throw new BusinessException("Company reached the store limit of its subscription plan");
        }
    }

    private Company findActiveCompanyById(Integer id) {
        return companyRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    private RetailStore findActiveStoreById(Integer id) {
        return retailStoreRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

    private RetailStore findStoreById(Integer id) {
        return retailStoreRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

    private void applyStoreFields(String name, String email, String phone, BigDecimal latitude, BigDecimal longitude, RetailStore store) {
        store.setName(name);
        store.setEmail(email);
        store.setPhone(phone);
        store.setLatitude(latitude);
        store.setLongitude(longitude);
    }

}
