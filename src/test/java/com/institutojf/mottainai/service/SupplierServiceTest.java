package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSupplierRequest;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.SupplierMapper;
import com.institutojf.mottainai.security.ProtectedDataToken;
import com.institutojf.mottainai.model.Address;
import com.institutojf.mottainai.model.Supplier;
import com.institutojf.mottainai.repository.SupplierRepository;
import com.institutojf.mottainai.repository.SupplierProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private SupplierProductRepository supplierProductRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierService supplierService;

    @Test
    @DisplayName("Should reject supplier when CNPJ already exists")
    void shouldRejectSupplierWhenCnpjAlreadyExists() {
        CreateSupplierRequest request = request();
        when(supplierRepository.existsByCnpj(request.cnpj())).thenReturn(true);

        assertThrows(ConflictException.class, () -> supplierService.create(request));
        verify(addressService, never()).createAddress(any());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject supplier when address does not exist")
    void shouldRejectSupplierWhenAddressDoesNotExist() {
        CreateSupplierRequest request = request();
        when(supplierRepository.existsByCnpj(request.cnpj())).thenReturn(false);
        when(addressService.createAddress(request.address())).thenThrow(new ResourceNotFoundException("Address not found"));
        assertThrows(ResourceNotFoundException.class, () -> supplierService.create(request));

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should deactivate supplier and mark it logically deleted")
    void shouldDeactivateSupplierAndMarkItLogicallyDeleted() {
        Supplier supplier = supplier(1, true);
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(supplier));

        supplierService.delete(1);

        assertFalse(supplier.getActive());
        assertNotNull(supplier.getDeletedAt());
        verify(supplierRepository).save(supplier);
    }

    @Test
    @DisplayName("Should reject supplier deactivation when it has active product links")
    void shouldRejectSupplierDeactivationWhenItHasActiveProductLinks() {
        Supplier supplier = supplier(1, true);
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(supplier));
        when(supplierProductRepository.existsBySupplier_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(true);

        assertThrows(BusinessException.class, () -> supplierService.delete(1));

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return not found when deactivating a nonexistent supplier")
    void shouldReturnNotFoundWhenDeactivatingANonexistentSupplier() {
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierService.delete(1));

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update supplier fields and reactivate it")
    void shouldUpdateSupplierFieldsAndReactivateIt() {
        Supplier supplier = supplier(1, false);
        Address address = new Address();
        address.setId(2);
        when(supplierRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(supplier));
        when(addressService.updateAddress(eq(1), any())).thenReturn(address);
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        supplierService.update(1, new UpdateSupplierRequest(updateAddressRequest(), "Fornecedor Atualizado", "novo@fornecedor.com", "11888888888", true));

        assertEquals(1, supplier.getAddress().getId());
        assertEquals("Fornecedor Atualizado", supplier.getTradeName());
        assertEquals(ProtectedDataToken.email("novo@fornecedor.com"), supplier.getEmail());
        assertEquals(ProtectedDataToken.phone("11888888888"), supplier.getPhone());
        assertTrue(supplier.getActive());

        verify(addressService).updateAddress(eq(1), any());
        verify(supplierRepository).save(supplier);
    }

    private Supplier supplier(Integer id, boolean active) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setActive(active);
        supplier.setAddress(address());
        return supplier;
    }

    private Address address() {
        Address address = new Address();
        address.setId(1);
        return address;
    }

    private CreateSupplierRequest request() {
        return new CreateSupplierRequest(createAddressRequest(), "Fornecedor Teste", "11222333000181", "fornecedor@teste.com", "11999999999");
    }

    private CreateAddressRequest createAddressRequest() {
        return new CreateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
    }

    private UpdateAddressRequest updateAddressRequest() {
        return new UpdateAddressRequest("05120060", "Rua Irineu José Bordon", "335", null, "Vila Jaguara", "São Paulo", "SP");
    }

}
