package com.institutojf.mottainai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.institutojf.mottainai.dto.request.CreateDamageReportRequest;
import com.institutojf.mottainai.dto.response.DamageReportResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.DamageReport;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.repository.DamageReportRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class DamageReportServiceTest {
    private DamageReportRepository reports;
    private ProductRepository products;
    private InventoryAccess access;
    private DamageReportService service;
    private Authentication authentication;
    private RetailStore store;
    private Product product;

    @BeforeEach void setUp() {
        reports = mock(DamageReportRepository.class);
        products = mock(ProductRepository.class);
        access = mock(InventoryAccess.class);
        service = new DamageReportService(reports, products, access);
        authentication = mock(Authentication.class);
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(4);
        store = mock(RetailStore.class);
        when(store.getId()).thenReturn(12);
        when(store.getCompany()).thenReturn(company);
        Employee employee = mock(Employee.class);
        when(employee.getId()).thenReturn(33);
        when(employee.getStore()).thenReturn(store);
        AppUser actor = mock(AppUser.class);
        when(actor.getEmployee()).thenReturn(employee);
        when(access.currentUser(authentication)).thenReturn(actor);
        product = mock(Product.class);
        when(product.getId()).thenReturn(90);
        when(product.getCompany()).thenReturn(company);
        when(products.findByIdAndDeletedAtIsNull(90)).thenReturn(Optional.of(product));
        when(reports.save(any(DamageReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void persistsDamageImageAndUsesAuthenticatedStoreAndEmployee() {
        String url = "https://res.cloudinary.com/demo/image/upload/v1/damage.jpg";
        DamageReportResponse response = service.create(new CreateDamageReportRequest(90, "quebrado",
                new BigDecimal("2.000"), "embalagem rompida", url), authentication);
        assertEquals(12, response.storeId());
        assertEquals(33, response.employeeId());
        assertEquals(90, response.productId());
        assertEquals(url, response.photoUrl());
    }

    @Test void hidesProductsFromAnotherCompany() {
        Company foreign = mock(Company.class);
        when(foreign.getId()).thenReturn(99);
        when(product.getCompany()).thenReturn(foreign);
        assertThrows(ResourceNotFoundException.class, () -> service.create(
                new CreateDamageReportRequest(90, "quebrado", BigDecimal.ONE, null, null), authentication));
    }
}
