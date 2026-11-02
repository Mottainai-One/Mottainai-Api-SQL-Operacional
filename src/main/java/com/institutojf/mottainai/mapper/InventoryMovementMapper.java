package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.InventoryMovementResponse;
import com.institutojf.mottainai.model.InventoryMovement;
import org.springframework.stereotype.Component;

@Component
public class InventoryMovementMapper {

    public InventoryMovementResponse toResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getMovementId(),
                movement.getInventory().getId(),
                movement.getEmployee() == null ? null : movement.getEmployee().getId(),
                movement.getMovementDate(),
                movement.getMovementType(),
                movement.getMovedQuantity(),
                movement.getPreviousBalance(),
                movement.getCurrentBalance(),
                movement.getObservation(),
                movement.getStoreId()
        );
    }

}
