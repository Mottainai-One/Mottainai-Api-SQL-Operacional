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
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.ProductCategory;
import com.institutojf.mottainai.model.TaxProfile;
import com.institutojf.mottainai.repository.ProductCategoryRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.SupplierProductRepository;
import com.institutojf.mottainai.repository.TaxProfileRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final ProductCategoryRepository categoryRepository;

    private final TaxProfileRepository taxProfileRepository;

    private final SupplierProductRepository supplierProductRepository;

    private final ProductMapper productMapper;

    private final EntityManager entityManager;

    private final AppUserRepository appUserRepository;

    private final AuditLogRepository auditLogRepository;

    private final StaffUserLookup staffUserLookup;

    private final InventoryAccess inventoryAccess;

    @Transactional
    public ProductResponse create(CreateProductRequest request, Authentication authentication) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        Company company = actor.getEmployee().getStore().getCompany();
        if (productRepository.existsByCompany_IdAndBarcode(company.getId(), request.barcode())) {
            throw new ConflictException("Product barcode already exists");
        }
        Product product = new Product();
        product.setCompany(company);
        product.setBarcode(request.barcode());
        product.setActive(true);
        applyProductFields(request.categoryId(), request.taxProfileId(), request.ncm(), request.cest(), request.name(), request.description(), request.brand(), request.unitMeasure(), request.weight(), product);
        product.setImageUrl(request.imageUrl());
        Product savedProduct = productRepository.saveAndFlush(product);
        entityManager.refresh(savedProduct);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable).map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Integer id) {
        return productMapper.toResponse(findActiveProductById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findByBarcode(String barcode) {
        Product product = productRepository.findByBarcodeAndActiveTrueAndDeletedAtIsNull(barcode)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(Integer id, UpdateProductRequest request) {
        Product product = findProductById(id);
        if (!Objects.equals(product.getVersion(), request.version())) {
            throw new ConflictException("Product was updated by another request; reload it and retry");
        }
        if (Boolean.FALSE.equals(request.active())) {
            ensureCanDeactivate(id);
        }
        applyProductFields(request.categoryId(), request.taxProfileId(), request.ncm(), request.cest(), request.name(), request.description(), request.brand(), request.unitMeasure(), request.weight(), product);
        product.setActive(request.active());
        product.setImageUrl(request.imageUrl());
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Integer id, Boolean active) {
        Product product = findProductById(id);
        if (Boolean.FALSE.equals(active)) {
            ensureCanDeactivate(id);
        }
        product.setActive(active);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Integer id, String actorEmail) {
        Product product = findActiveProductById(id);
        ensureCanDeactivate(id);
        AppUser actor = staffUserLookup.findActive(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        product.setActive(false);
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        auditLogRepository.record("product", "DELETE", id.toString(), actor.getId(), Map.of("active", true),
                Map.of("active", false, "deleted_at", product.getDeletedAt().toString()));
    }

    private void ensureCanDeactivate(Integer id) {
        if (supplierProductRepository.existsByProduct_IdAndActiveTrueAndDeletedAtIsNull(id)) {
            throw new BusinessException("Product has active supplier links");
        }
    }

    private Product findActiveProductById(Integer id) {
        return productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Product findProductById(Integer id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void applyProductFields(Integer categoryId, Integer taxProfileId, String ncm, String cest, String name, String description, String brand, String unitMeasure, BigDecimal weight, Product product) {
        ProductCategory category = categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Product category not found"));
        TaxProfile taxProfile = taxProfileRepository.findByIdAndActiveTrueAndDeletedAtIsNull(taxProfileId)
            .orElseThrow(() -> new ResourceNotFoundException("Tax profile not found"));
        product.setCategory(category);
        product.setTaxProfile(taxProfile);
        product.setNcm(ncm);
        product.setCest(cest);
        product.setName(name);
        product.setDescription(description);
        product.setBrand(brand);
        product.setUnitMeasure(unitMeasure);
        product.setWeight(weight);
    }

}
