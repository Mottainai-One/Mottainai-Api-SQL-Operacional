package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateProductCategoryRequest;
import com.institutojf.mottainai.dto.request.UpdateProductCategoryRequest;
import com.institutojf.mottainai.dto.response.ProductCategoryResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.ProductCategoryMapper;
import com.institutojf.mottainai.model.ProductCategory;
import com.institutojf.mottainai.repository.ProductCategoryRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    private final ProductRepository productRepository;

    private final ProductCategoryMapper categoryMapper;

    @Transactional
    public ProductCategoryResponse create(CreateProductCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Product category name already exists");
        }
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setDescription(request.description());
        category.setActive(true);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable).map(categoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductCategoryResponse findById(Integer id) {
        return categoryMapper.toResponse(findActiveCategoryById(id));
    }

    @Transactional
    public ProductCategoryResponse update(Integer id, UpdateProductCategoryRequest request) {
        ProductCategory category = findCategoryById(id);
        String name = request.name().trim();
        categoryRepository.findByNameIgnoreCase(name)
            .filter(foundCategory -> !foundCategory.getId().equals(id))
            .ifPresent(foundCategory -> {
                throw new ConflictException("Product category name already exists");
            });
        if (Boolean.FALSE.equals(request.active())) {
            ensureCanDeactivate(id);
        }
        category.setName(name);
        category.setDescription(request.description());
        category.setActive(request.active());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Integer id) {
        ProductCategory category = findActiveCategoryById(id);
        ensureCanDeactivate(id);
        category.setActive(false);
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private void ensureCanDeactivate(Integer id) {
        if (productRepository.existsByCategory_IdAndActiveTrueAndDeletedAtIsNull(id)) {
            throw new BusinessException("Product category has active products");
        }
    }

    private ProductCategory findActiveCategoryById(Integer id) {
        return categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product category not found"));
    }

    private ProductCategory findCategoryById(Integer id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product category not found"));
    }

}
