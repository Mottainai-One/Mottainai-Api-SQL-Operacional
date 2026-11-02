package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSupplierRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierRequest;
import com.institutojf.mottainai.dto.response.SupplierResponse;
import com.institutojf.mottainai.dto.response.SupplierPurchaseHistoryResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.SupplierMapper;
import com.institutojf.mottainai.model.Supplier;
import com.institutojf.mottainai.repository.ProductCommercialRepository;
import com.institutojf.mottainai.repository.SupplierProductRepository;
import com.institutojf.mottainai.repository.SupplierRepository;
import com.institutojf.mottainai.security.ProtectedDataToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    private final AddressService addressService;

    private final SupplierProductRepository supplierProductRepository;

    private final SupplierMapper supplierMapper;

    private final ProductCommercialRepository productCommercialRepository;

    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        if (supplierRepository.existsByCnpj(request.cnpj())) {
            throw new ConflictException("Supplier CNPJ already exists");
        }
        Supplier supplier = new Supplier();
        supplier.setCnpj(request.cnpj());
        supplier.setActive(true);
        supplier.setAddress(addressService.createAddress(request.address()));
        applySupplierFields(request.tradeName(), request.email(), request.phone(), supplier);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> findAll(Pageable pageable) {
        return supplierRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable).map(supplierMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Integer id) {
        return supplierMapper.toResponse(findActiveSupplierById(id));
    }

    @Transactional(readOnly = true)
    public List<SupplierPurchaseHistoryResponse> findPurchaseHistory(Integer id, LocalDateTime from, LocalDateTime to) {
        findActiveSupplierById(id);
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusMonths(6))) {
            throw new BusinessException("A valid purchase history range of at most six months is required");
        }
        return productCommercialRepository.findSupplierPurchaseHistory(id, from, to);
    }

    @Transactional
    public SupplierResponse update(Integer id, UpdateSupplierRequest request) {
        Supplier supplier = findSupplierById(id);
        if (Boolean.FALSE.equals(request.active())) {
            ensureCanDeactivate(id);
        }
        addressService.updateAddress(supplier.getAddress().getId(), request.address());
        applySupplierFields(request.tradeName(), request.email(), request.phone(), supplier);
        supplier.setActive(request.active());
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Integer id) {
        Supplier supplier = findActiveSupplierById(id);
        ensureCanDeactivate(id);
        supplier.setActive(false);
        supplier.setDeletedAt(LocalDateTime.now());
        supplierRepository.save(supplier);
    }

    private void ensureCanDeactivate(Integer id) {
        if (supplierProductRepository.existsBySupplier_IdAndActiveTrueAndDeletedAtIsNull(id)) {
            throw new BusinessException("Supplier has active product links");
        }
    }

    private Supplier findActiveSupplierById(Integer id) {
        return supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    private void applySupplierFields(String tradeName, String email, String phone, Supplier supplier) {
        supplier.setTradeName(tradeName);
        supplier.setEmail(ProtectedDataToken.email(email));
        supplier.setPhone(ProtectedDataToken.phone(phone));
    }

    private Supplier findSupplierById(Integer id) {
        return supplierRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

}
