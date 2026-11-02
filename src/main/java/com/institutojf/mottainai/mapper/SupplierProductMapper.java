package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.SupplierProductResponse;
import com.institutojf.mottainai.model.SupplierProduct;
import org.springframework.stereotype.Component;

/**
 * Transforma a relação entre fornecedor e produto no formato de resposta da API
 */
@Component
public class SupplierProductMapper {

    public SupplierProductResponse toResponse(SupplierProduct supplierProduct) {
        return new SupplierProductResponse(supplierProduct.getId(), supplierProduct.getSupplier().getId(),
                supplierProduct.getSupplier().getTradeName(), supplierProduct.getProduct().getId(),
                supplierProduct.getProduct().getName(), supplierProduct.getSupplierCode(),
                supplierProduct.getPurchasePrice(), supplierProduct.getLeadTime(), supplierProduct.getActive());
    }

}
