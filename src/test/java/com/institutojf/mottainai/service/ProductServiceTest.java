package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateProductRequest;
import com.institutojf.mottainai.dto.request.UpdateProductRequest;
import com.institutojf.mottainai.dto.response.ProductResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.ProductMapper;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.ProductCategory;
import com.institutojf.mottainai.model.TaxProfile;
import com.institutojf.mottainai.repository.ProductCategoryRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.SupplierProductRepository;
import com.institutojf.mottainai.repository.TaxProfileRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private TaxProfileRepository taxProfileRepository;

    @Mock
    private SupplierProductRepository supplierProductRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private InventoryAccess inventoryAccess;

    private final Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        when(inventoryAccess.currentUser(any())).thenReturn(actor());
    }

    @Test
    @DisplayName("Should refresh product after database generates SKU")
    void shouldRefreshProductAfterDatabaseGeneratesSku() {
        Product product = product(1, true);
        ProductResponse response = response();
        when(productRepository.existsByCompany_IdAndBarcode(1, "123")).thenReturn(false);
        when(categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(category()));
        when(taxProfileRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(taxProfile()));
        when(productRepository.saveAndFlush(any())).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.create(createRequest(), authentication);

        assertEquals(response, result);
        verify(entityManager).refresh(product);
    }

    @Test
    @DisplayName("Should reject product when barcode already exists")
    void shouldRejectProductWhenBarcodeAlreadyExists() {
        when(productRepository.existsByCompany_IdAndBarcode(1, "123")).thenReturn(true);

        assertThrows(ConflictException.class, () -> productService.create(createRequest(), authentication));

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should reject product when tax profile does not exist")
    void shouldRejectProductWhenTaxProfileDoesNotExist() {
        when(productRepository.existsByCompany_IdAndBarcode(1, "123")).thenReturn(false);
        when(categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(category()));
        when(taxProfileRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.create(createRequest(), authentication));

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should update product fields and reactivate it")
    void shouldUpdateProductFieldsAndReactivateIt() {
        Product product = product(1, false);
        when(productRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(category()));
        when(taxProfileRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(taxProfile()));
        when(productRepository.save(product)).thenReturn(product);
        productService.update(1, new UpdateProductRequest(1, 1, "12345678", null, "Brown rice", "Whole grain", "Mottainai", "KG", new BigDecimal("1.25"), true, 1));
        assertEquals("12345678", product.getNcm());
        assertEquals("Brown rice", product.getName());
        assertTrue(product.getActive());
    }

    @Test
    @DisplayName("Should reject product deactivation when it has active supplier links")
    void shouldRejectProductDeactivationWhenItHasActiveSupplierLinks() {
        Product product = product(1, true);
        when(productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(product));
        when(supplierProductRepository.existsByProduct_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(true);

        assertThrows(BusinessException.class, () -> productService.delete(1, "admin@example.com"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should change product status without updating product fields")
    void shouldChangeProductStatusWithoutUpdatingProductFields() {
        Product product = product(1, true);
        when(productRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        productService.updateStatus(1, false);
        assertEquals(false, product.getActive());
        assertEquals("Rice", product.getName());
        verify(productRepository).save(product);
    }

    private CreateProductRequest createRequest() {
        return new CreateProductRequest(1, 1, "123", "12345678", null, "Rice", null, null, "KG", BigDecimal.ONE);
    }

    private Product product(Integer id, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setCompany(company());
        product.setCategory(category());
        product.setTaxProfile(taxProfile());
        product.setSku("RIC-001");
        product.setBarcode("123");
        product.setNcm("12345678");
        product.setName("Rice");
        product.setUnitMeasure("KG");
        product.setActive(active);
        product.setVersion(1);
        return product;
    }

    private AppUser actor() {
        AppUser user = new AppUser();
        Employee employee = new Employee();
        RetailStore store = new RetailStore();
        store.setCompany(company());
        employee.setStore(store);
        user.setEmployee(employee);
        return user;
    }

    private Company company() {
        Company company = new Company();
        company.setId(1);
        return company;
    }

    private ProductCategory category() {
        ProductCategory category = new ProductCategory();
        category.setId(1);
        category.setName("Food");
        category.setActive(true);
        return category;
    }

    private TaxProfile taxProfile() {
        TaxProfile taxProfile = new TaxProfile();
        taxProfile.setId(1);
        taxProfile.setCode("STANDARD");
        taxProfile.setName("Standard");
        taxProfile.setActive(true);
        return taxProfile;
    }

    private ProductResponse response() {
        return new ProductResponse(1, 1, 1, "Food", 1, "RIC-001", "123", "12345678", null, "Rice", null, null, "KG", BigDecimal.ONE, true, 1);
    }

}
