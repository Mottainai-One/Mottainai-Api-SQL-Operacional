package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.ProductCategoryControllerApi;
import com.institutojf.mottainai.dto.request.CreateProductCategoryRequest;
import com.institutojf.mottainai.dto.request.UpdateProductCategoryRequest;
import com.institutojf.mottainai.dto.response.ProductCategoryResponse;
import com.institutojf.mottainai.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController implements ProductCategoryControllerApi {

    private final ProductCategoryService categoryService;

    @Override
    @PostMapping
    public ResponseEntity<ProductCategoryResponse> create(@Valid @RequestBody CreateProductCategoryRequest request) {
        ProductCategoryResponse category = categoryService.create(request);
        URI location = URI.create("/api/v1/product-categories/" + category.id());
        return ResponseEntity.created(location).body(category);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<ProductCategoryResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(categoryService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateProductCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
