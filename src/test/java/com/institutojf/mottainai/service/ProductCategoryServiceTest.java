package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateProductCategoryRequest;
import com.institutojf.mottainai.dto.request.UpdateProductCategoryRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.ProductCategoryMapper;
import com.institutojf.mottainai.model.ProductCategory;
import com.institutojf.mottainai.repository.ProductCategoryRepository;
import com.institutojf.mottainai.repository.ProductRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryMapper categoryMapper;

    @InjectMocks
    private ProductCategoryService productCategoryService;

    @Test
    @DisplayName("Should reject category when name already exists")
    void shouldRejectCategoryWhenNameAlreadyExists() {
        when(categoryRepository.existsByNameIgnoreCase("Food")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> productCategoryService.create(new CreateProductCategoryRequest("Food", "Food products")));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject category update when name belongs to another category")
    void shouldRejectCategoryUpdateWhenNameBelongsToAnotherCategory() {
        ProductCategory category = category(1, true);
        ProductCategory duplicate = category(2, true);
        duplicate.setName("Drinks");
        when(categoryRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Drinks")).thenReturn(Optional.of(duplicate));
        assertThrows(ConflictException.class,
                () -> productCategoryService.update(1, new UpdateProductCategoryRequest("Drinks", "Beverages", true)));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should deactivate category and mark it logically deleted")
    void shouldDeactivateCategoryAndMarkItLogicallyDeleted() {
        ProductCategory category = category(1, true);
        when(categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(category));
        productCategoryService.delete(1);
        assertFalse(category.getActive());
        assertNotNull(category.getDeletedAt());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("Should reject category deactivation when it has active products")
    void shouldRejectCategoryDeactivationWhenItHasActiveProducts() {
        ProductCategory category = category(1, true);
        when(categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategory_IdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(true);
        assertThrows(BusinessException.class, () -> productCategoryService.delete(1));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return not found when deactivating a nonexistent category")
    void shouldReturnNotFoundWhenDeactivatingANonexistentCategory() {
        when(categoryRepository.findByIdAndActiveTrueAndDeletedAtIsNull(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productCategoryService.delete(1));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update category fields and reactivate it")
    void shouldUpdateCategoryFieldsAndReactivateIt() {
        ProductCategory category = category(1, false);
        when(categoryRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);
        productCategoryService.update(1, new UpdateProductCategoryRequest("Dry food", "Shelf-stable food", true));
        assertEquals("Dry food", category.getName());
        assertEquals("Shelf-stable food", category.getDescription());
        assertTrue(category.getActive());
        verify(categoryRepository).save(category);
    }

    private ProductCategory category(Integer id, boolean active) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setName("Food");
        category.setActive(active);
        return category;
    }

}
