package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.ProductCategoryResponse;
import com.institutojf.mottainai.model.ProductCategory;
import org.springframework.stereotype.Component;

/**
 * Transforma uma categoria no formato de resposta da API
 */
@Component
public class ProductCategoryMapper {

    public ProductCategoryResponse toResponse(ProductCategory category) {
        return new ProductCategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.getActive());
    }

}
