package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.ProductResponse;
import com.institutojf.mottainai.model.Product;
import org.springframework.stereotype.Component;

/**
 * Transforma um produto no formato de resposta da API
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getCompany().getId(), product.getCategory().getId(), product.getCategory().getName(),
                product.getTaxProfile().getId(), product.getSku(), product.getBarcode(), product.getNcm(),
                product.getCest(), product.getName(), product.getDescription(), product.getBrand(),
                product.getUnitMeasure(), product.getWeight(), product.getActive(), product.getVersion(), product.getImageUrl());
    }

}
