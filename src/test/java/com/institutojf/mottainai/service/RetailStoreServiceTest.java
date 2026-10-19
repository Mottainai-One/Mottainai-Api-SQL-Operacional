package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateRetailStoreRequest;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateRetailStoreRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.RetailStoreMapper;
import com.institutojf.mottainai.model.Address;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.SubscriptionPlan;
import com.institutojf.mottainai.repository.CompanyRepository;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetailStoreServiceTest {

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StaffUserLookup staffUserLookup;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private RetailStoreMapper retailStoreMapper;

    @InjectMocks
    private RetailStoreService retailStoreService;

    @Test
    @DisplayName("Should create store when the plan limit allows it")
    void shouldCreateStoreWhenThePlanLimitAllowsIt() {
        when(retailStoreRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(companyRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(company(true)));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(1L);
        when(addressService.createAddress(any())).thenReturn(address());
        when(retailStoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        retailStoreService.create(createRequest());
        verify(retailStoreRepository).save(any());
    }

    @Test
    @DisplayName("Should reject store when CNPJ already exists")
    void shouldRejectStoreWhenCnpjAlreadyExists() {
        when(retailStoreRepository.existsByCnpj("11222333000181")).thenReturn(true);
        assertThrows(ConflictException.class, () -> retailStoreService.create(createRequest()));
        verify(retailStoreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject store when the company reached its plan store limit")
    void shouldRejectStoreWhenTheCompanyReachedItsPlanStoreLimit() {
        when(retailStoreRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(companyRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(company(true)));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(2L);
        assertThrows(BusinessException.class, () -> retailStoreService.create(createRequest()));
        verify(retailStoreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return not found when the address does not exist")
    void shouldReturnNotFoundWhenTheAddressDoesNotExist() {
        when(retailStoreRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(companyRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(company(true)));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(0L);
        when(addressService.createAddress(any())).thenThrow(new ResourceNotFoundException("Address not found"));
        assertThrows(ResourceNotFoundException.class, () -> retailStoreService.create(createRequest()));
        verify(retailStoreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject store reactivation when the plan store limit is reached")
    void shouldRejectStoreReactivationWhenThePlanStoreLimitIsReached() {
        when(retailStoreRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(store(false)));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(2L);
        assertThrows(BusinessException.class, () -> retailStoreService.update(1, updateRequest(true)));
        verify(retailStoreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update store fields")
    void shouldUpdateStoreFields() {
        RetailStore store = store(true);
        when(retailStoreRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(store));
        when(addressService.updateAddress(eq(1), any())).thenReturn(address());
        when(retailStoreRepository.save(store)).thenReturn(store);
        retailStoreService.update(1, updateRequest(true));
        assertEquals("Loja Centro", store.getName());
        assertEquals("loja@mottainai.com", store.getEmail());
        verify(retailStoreRepository).save(store);
    }

    @Test
    @DisplayName("Should soft delete store")
    void shouldSoftDeleteStore() {
        RetailStore store = store(true);
        AppUser actor = new AppUser();
        actor.setId(9);
        when(retailStoreRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(store));
        when(staffUserLookup.findActive("admin@example.com")).thenReturn(Optional.of(actor));
        retailStoreService.delete(1, "admin@example.com");
        assertFalse(store.getActive());
        assertNotNull(store.getDeletedAt());
        verify(retailStoreRepository).save(store);
    }

    @Test
    @DisplayName("Should reactivate store through the status flow when the plan allows it")
    void shouldReactivateStoreThroughTheStatusFlowWhenThePlanAllowsIt() {
        RetailStore store = store(false);
        when(retailStoreRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(store));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(1L);
        when(retailStoreRepository.save(store)).thenReturn(store);
        retailStoreService.updateStatus(1, true);
        assertEquals(true, store.getActive());
        verify(retailStoreRepository).save(store);
    }

    private CreateRetailStoreRequest createRequest() {
        return new CreateRetailStoreRequest(1, createAddressRequest(), "Loja Centro", "11222333000181", "loja@mottainai.com", "11999999999",
                null, null);
    }

    private UpdateRetailStoreRequest updateRequest(boolean active) {
        return new UpdateRetailStoreRequest(updateAddressRequest(), "Loja Centro", "loja@mottainai.com", "11999999999", null, null, active);
    }

    private CreateAddressRequest createAddressRequest() {
        return new CreateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
    }

    private UpdateAddressRequest updateAddressRequest() {
        return new UpdateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
    }

    private SubscriptionPlan plan() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(1);
        plan.setName("Basic");
        plan.setPrice(new BigDecimal("99.90"));
        plan.setStoreLimit(2);
        plan.setUserLimit(5);
        plan.setActive(true);
        return plan;
    }

    private Company company(boolean active) {
        Company company = new Company();
        company.setId(1);
        company.setPlan(plan());
        company.setOfficialName("Mottainai Comercio LTDA");
        company.setCnpj("11222333000181");
        company.setActive(active);
        return company;
    }

    private Address address() {
        Address address = new Address();
        address.setId(1);
        address.setZipCode("05120060");
        address.setStreet("Rua Irineu José Bordon");
        address.setNumber("335");
        address.setNeighborhood("Vila Jaguara");
        address.setCity("São Paulo");
        address.setState("SP");
        return address;
    }

    private RetailStore store(boolean active) {
        RetailStore store = new RetailStore();
        store.setId(1);
        store.setCompany(company(true));
        store.setAddress(address());
        store.setName("Loja Centro");
        store.setCnpj("11222333000181");
        store.setActive(active);
        return store;
    }

}
