package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSupplierProductRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierProductRequest;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.SupplierProductMapper;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.Supplier;
import com.institutojf.mottainai.model.SupplierProduct;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.SupplierProductRepository;
import com.institutojf.mottainai.repository.SupplierRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.security.InventoryAccess;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierProductServiceTest {

    @Mock
    private SupplierProductRepository supplierProductRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierProductMapper supplierProductMapper;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @InjectMocks
    private SupplierProductService supplierProductService;

    @Test
    @DisplayName("Should reject duplicate supplier product link")
    void shouldRejectDuplicateSupplierProductLink() {
        CreateSupplierProductRequest request = request();
        when(supplierProductRepository.existsBySupplier_IdAndProduct_Id(1, 1)).thenReturn(true);
        assertThrows(ConflictException.class, () -> supplierProductService.create(request));
        verify(supplierRepository, never()).findByIdAndActiveTrueAndDeletedAtIsNull(any());
        verify(supplierProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject supplier product link when supplier does not exist")
    void shouldRejectSupplierProductLinkWhenSupplierDoesNotExist() {
        CreateSupplierProductRequest request = request();
        when(supplierProductRepository.existsBySupplier_IdAndProduct_Id(1, 1)).thenReturn(false);
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> supplierProductService.create(request));
        verify(productRepository, never()).findByIdAndActiveTrueAndDeletedAtIsNull(any());
        verify(supplierProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject supplier product link when product does not exist")
    void shouldRejectSupplierProductLinkWhenProductDoesNotExist() {
        CreateSupplierProductRequest request = request();
        Supplier supplier = new Supplier();
        supplier.setId(1);
        when(supplierProductRepository.existsBySupplier_IdAndProduct_Id(1, 1)).thenReturn(false);
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(supplier));
        when(productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> supplierProductService.create(request));
        verify(supplierProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should deactivate supplier product link without soft deleting it")
    void shouldDeactivateSupplierProductLinkWithoutSoftDeletingIt() {
        SupplierProduct supplierProduct = supplierProduct(1, true);
        when(supplierProductRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1))
            .thenReturn(Optional.of(supplierProduct));
        supplierProductService.deactivate(1);
        assertFalse(supplierProduct.getActive());
        assertNull(supplierProduct.getDeletedAt());
        verify(supplierProductRepository).save(supplierProduct);
    }

    @Test
    @DisplayName("Should return not found when deactivating a nonexistent supplier product link")
    void shouldReturnNotFoundWhenDeactivatingANonexistentSupplierProductLink() {
        when(supplierProductRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> supplierProductService.deactivate(1));
        verify(supplierProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update supplier product link fields and reactivate it")
    void shouldUpdateSupplierProductLinkFieldsAndReactivateIt() {
        SupplierProduct supplierProduct = supplierProduct(1, false);
        when(supplierProductRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(supplierProduct));
        when(supplierProductRepository.save(supplierProduct)).thenReturn(supplierProduct);
        supplierProductService.update(1, new UpdateSupplierProductRequest("FORN-002", new BigDecimal("12.50"), 5, true));
        assertEquals("FORN-002", supplierProduct.getSupplierCode());
        assertEquals(new BigDecimal("12.50"), supplierProduct.getPurchasePrice());
        assertEquals(5, supplierProduct.getLeadTime());
        assertTrue(supplierProduct.getActive());
        verify(supplierProductRepository).save(supplierProduct);
    }

    @Test
    @DisplayName("Should reactivate an existing inactive link from a nested route")
    void shouldReactivateExistingInactiveLink() {
        Supplier supplier = new Supplier();
        supplier.setId(1);
        Product product = new Product();
        product.setId(2);
        SupplierProduct link = supplierProduct(3, false);
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(supplier));
        when(productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(2)).thenReturn(Optional.of(product));
        when(supplierProductRepository.findBySupplier_IdAndProduct_IdAndDeletedAtIsNull(1, 2))
            .thenReturn(Optional.of(link));
        when(supplierProductRepository.save(link)).thenReturn(link);
        supplierProductService.link(1, 2, "CODE", new BigDecimal("9.00"), 2);
        assertTrue(link.getActive());
        assertEquals("CODE", link.getSupplierCode());
        assertEquals(new BigDecimal("9.00"), link.getPurchasePrice());
        verify(supplierProductRepository).save(link);
    }

    private SupplierProduct supplierProduct(Integer id, boolean active) {
        SupplierProduct supplierProduct = new SupplierProduct();
        supplierProduct.setId(id);
        supplierProduct.setActive(active);
        return supplierProduct;
    }

    private CreateSupplierProductRequest request() {
        return new CreateSupplierProductRequest(1, 1, "FORN-001", new BigDecimal("10.00"), 3);
    }

}
