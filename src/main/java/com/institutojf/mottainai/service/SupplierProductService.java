package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateSupplierProductRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierProductRequest;
import com.institutojf.mottainai.dto.response.SupplierProductResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;

    private final SupplierRepository supplierRepository;

    private final ProductRepository productRepository;

    private final SupplierProductMapper supplierProductMapper;

    private final AuditLogRepository auditLogRepository;

    private final InventoryAccess inventoryAccess;

    @Transactional
    public SupplierProductResponse create(CreateSupplierProductRequest request) {
        if (supplierProductRepository.existsBySupplier_IdAndProduct_Id(request.supplierId(), request.productId())) {
            throw new ConflictException("Supplier is already linked to this product");
        }
        Supplier supplier = supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        Product product = productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        SupplierProduct supplierProduct = new SupplierProduct();
        supplierProduct.setSupplier(supplier);
        supplierProduct.setProduct(product);
        supplierProduct.setSupplierCode(request.supplierCode());
        supplierProduct.setPurchasePrice(request.purchasePrice());
        supplierProduct.setLeadTime(request.leadTime());
        supplierProduct.setActive(true);
        return supplierProductMapper.toResponse(supplierProductRepository.save(supplierProduct));
    }

    @Transactional(readOnly = true)
    public Page<SupplierProductResponse> findAll(Pageable pageable) {
        return supplierProductRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable)
            .map(supplierProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SupplierProductResponse> findByProduct(Integer productId, Pageable pageable) {
        requireProduct(productId);
        return supplierProductRepository.findAllByProduct_IdAndActiveTrueAndDeletedAtIsNull(productId, pageable)
            .map(supplierProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SupplierProductResponse> findBySupplier(Integer supplierId, Pageable pageable) {
        requireSupplier(supplierId);
        return supplierProductRepository.findAllBySupplier_IdAndActiveTrueAndDeletedAtIsNull(supplierId, pageable)
            .map(supplierProductMapper::toResponse);
    }

    @Transactional
    public SupplierProductResponse link(Integer supplierId, Integer productId, String supplierCode, BigDecimal purchasePrice, Integer leadTime) {
        Supplier supplier = requireSupplier(supplierId);
        Product product = requireProduct(productId);
        SupplierProduct link = supplierProductRepository
            .findBySupplier_IdAndProduct_IdAndDeletedAtIsNull(supplierId, productId)
            .orElseGet(SupplierProduct::new);
        if (link.getId() != null && Boolean.TRUE.equals(link.getActive())) {
            throw new ConflictException("Supplier is already linked to this product");
        }
        link.setSupplier(supplier);
        link.setProduct(product);
        applyTerms(link, supplierCode, purchasePrice, leadTime);
        link.setActive(true);
        return supplierProductMapper.toResponse(supplierProductRepository.save(link));
    }

    @Transactional
    public SupplierProductResponse link(Integer supplierId, Integer productId, String supplierCode, BigDecimal purchasePrice, Integer leadTime, Authentication authentication) {
        var actor = inventoryAccess.currentUser(authentication);
        SupplierProductResponse created = link(supplierId, productId, supplierCode, purchasePrice, leadTime);
        auditLogRepository.record("supplier_product", "INSERT", created.id().toString(), actor.getId(), null, created);
        return created;
    }

    @Transactional
    public SupplierProductResponse update(Integer supplierId, Integer productId, UpdateSupplierProductRequest request) {
        SupplierProduct link = findByPair(supplierId, productId);
        applyTerms(link, request.supplierCode(), request.purchasePrice(), request.leadTime());
        link.setActive(request.active());
        return supplierProductMapper.toResponse(supplierProductRepository.save(link));
    }

    @Transactional
    public SupplierProductResponse update(Integer supplierId, Integer productId, UpdateSupplierProductRequest request, Authentication authentication) {
        var actor = inventoryAccess.currentUser(authentication);
        SupplierProductResponse old = supplierProductMapper.toResponse(findByPair(supplierId, productId));
        SupplierProductResponse updated = update(supplierId, productId, request);
        auditLogRepository.record("supplier_product", "UPDATE", updated.id().toString(), actor.getId(), old, updated);
        return updated;
    }

    @Transactional
    public void deactivate(Integer supplierId, Integer productId) {
        SupplierProduct link = findByPair(supplierId, productId);
        if (!Boolean.TRUE.equals(link.getActive())) {
            throw new ResourceNotFoundException("Supplier-product link not found");
        }
        link.setActive(false);
        supplierProductRepository.save(link);
    }

    @Transactional
    public void deactivate(Integer supplierId, Integer productId, Authentication authentication) {
        var actor = inventoryAccess.currentUser(authentication);
        SupplierProductResponse old = supplierProductMapper.toResponse(findByPair(supplierId, productId));
        deactivate(supplierId, productId);
        SupplierProductResponse updated = supplierProductMapper.toResponse(findByPair(supplierId, productId));
        auditLogRepository.record("supplier_product", "UPDATE", old.id().toString(), actor.getId(), old, updated);
    }

    @Transactional(readOnly = true)
    public SupplierProductResponse findById(Integer id) {
        return supplierProductMapper.toResponse(findActiveSupplierProductById(id));
    }

    @Transactional
    public SupplierProductResponse update(Integer id, UpdateSupplierProductRequest request) {
        SupplierProduct supplierProduct = findSupplierProductById(id);
        supplierProduct.setSupplierCode(request.supplierCode());
        supplierProduct.setPurchasePrice(request.purchasePrice());
        supplierProduct.setLeadTime(request.leadTime());
        supplierProduct.setActive(request.active());
        return supplierProductMapper.toResponse(supplierProductRepository.save(supplierProduct));
    }

    @Transactional
    public void deactivate(Integer id) {
        SupplierProduct supplierProduct = findActiveSupplierProductById(id);
        supplierProduct.setActive(false);
        supplierProductRepository.save(supplierProduct);
    }

    private SupplierProduct findActiveSupplierProductById(Integer id) {
        return supplierProductRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier product link not found"));
    }

    private SupplierProduct findSupplierProductById(Integer id) {
        return supplierProductRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier product link not found"));
    }

    private SupplierProduct findByPair(Integer supplierId, Integer productId) {
        return supplierProductRepository.findBySupplier_IdAndProduct_IdAndDeletedAtIsNull(supplierId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier-product link not found"));
    }

    private Supplier requireSupplier(Integer supplierId) {
        return supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    private Product requireProduct(Integer productId) {
        return productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void applyTerms(SupplierProduct link, String supplierCode, BigDecimal purchasePrice, Integer leadTime) {
        link.setSupplierCode(supplierCode);
        link.setPurchasePrice(purchasePrice);
        link.setLeadTime(leadTime);
    }

}
