package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateCompanyRequest;
import com.institutojf.mottainai.dto.request.UpdateCompanyRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.CompanyMapper;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.SubscriptionPlan;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.CompanyRepository;
import com.institutojf.mottainai.repository.EmployeeRepository;
import com.institutojf.mottainai.repository.RetailStoreRepository;
import com.institutojf.mottainai.repository.SubscriptionPlanRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private RetailStoreRepository retailStoreRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StaffUserLookup staffUserLookup;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("Should create company when CNPJ is available")
    void shouldCreateCompanyWhenCnpjIsAvailable() {
        when(companyRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(plan()));
        when(companyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        companyService.create(createRequest());

        verify(companyRepository).save(any());
    }

    @Test
    @DisplayName("Should reject company when CNPJ already exists")
    void shouldRejectCompanyWhenCnpjAlreadyExists() {
        when(companyRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(ConflictException.class, () -> companyService.create(createRequest()));

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return not found when the subscription plan is unavailable")
    void shouldReturnNotFoundWhenTheSubscriptionPlanIsUnavailable() {
        when(companyRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> companyService.create(createRequest()));

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject plan downgrade when active stores exceed the new limit")
    void shouldRejectPlanDowngradeWhenActiveStoresExceedTheNewLimit() {
        Company company = company(1, true);
        SubscriptionPlan smallerPlan = plan();
        smallerPlan.setStoreLimit(1);
        when(companyRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(company));
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1))
            .thenReturn(Optional.of(smallerPlan));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(3L);

        assertThrows(BusinessException.class, () -> companyService.update(1, updateRequest(true)));

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update company fields")
    void shouldUpdateCompanyFields() {
        Company company = company(1, true);
        when(companyRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(company));
        when(subscriptionPlanRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(plan()));
        when(retailStoreRepository.countByCompany_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(1L);
        when(companyRepository.save(company)).thenReturn(company);

        companyService.update(1, updateRequest(true));

        assertEquals("Mottainai Comercio LTDA", company.getOfficialName());
        assertEquals("contato@mottainai.com", company.getEmail());
        verify(companyRepository).save(company);
    }

    @Test
    @DisplayName("Should soft delete company and its organizational hierarchy")
    void shouldSoftDeleteCompanyAndItsOrganizationalHierarchy() {
        Company company = company(1, true);
        RetailStore store = new RetailStore();
        store.setActive(true);
        Employee employee = new Employee();
        employee.setActive(true);
        AppUser user = new AppUser();
        user.setId(9);
        user.setActive(true);
        when(staffUserLookup.findActive("admin@example.com")).thenReturn(Optional.of(user));
        when(companyRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(company));
        when(retailStoreRepository.findAllByCompany_IdAndDeletedAtIsNull(1)).thenReturn(java.util.List.of(store));
        when(employeeRepository.findAllByStore_Company_IdAndDeletedAtIsNull(1)).thenReturn(java.util.List.of(employee));
        when(appUserRepository.findAllByEmployee_Store_Company_IdAndDeletedAtIsNull(1)).thenReturn(java.util.List.of(user));

        companyService.delete(1, "admin@example.com");

        assertFalse(company.getActive());
        assertNotNull(company.getDeletedAt());
        assertFalse(store.getActive());
        assertNotNull(store.getDeletedAt());
        assertFalse(employee.getActive());
        assertNotNull(employee.getDeletedAt());
        assertFalse(user.getActive());
        assertNotNull(user.getDeletedAt());
        verify(companyRepository).save(company);
    }

    private CreateCompanyRequest createRequest() {
        return new CreateCompanyRequest(1, "Mottainai Comercio LTDA", "Mottainai", "11222333000181", "contato@mottainai.com", "11999999999", null, null);
    }

    private UpdateCompanyRequest updateRequest(boolean active) {
        return new UpdateCompanyRequest(1, "Mottainai Comercio LTDA", "Mottainai", "contato@mottainai.com", "11999999999", null, null, active);
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

    private Company company(Integer id, boolean active) {
        Company company = new Company();
        company.setId(id);
        company.setPlan(plan());
        company.setOfficialName("Mottainai Comercio LTDA");
        company.setCnpj("11222333000181");
        company.setEmail("contato@mottainai.com");
        company.setActive(active);
        return company;
    }

}
