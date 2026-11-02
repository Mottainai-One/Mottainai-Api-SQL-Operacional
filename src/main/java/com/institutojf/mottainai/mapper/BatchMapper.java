package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.BatchResponse;
import com.institutojf.mottainai.model.Batch;
import org.springframework.stereotype.Component;

@Component
public class BatchMapper {

    public BatchResponse toResponse(Batch batch) {
        return new BatchResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getBatchCode(),
                batch.getManufactureDate(),
                batch.getExpirationDate(),
                batch.getInitialQuantity(),
                batch.getUnitCost(),
                batch.getActive(),
                batch.getVersion()
        );
    }

}
