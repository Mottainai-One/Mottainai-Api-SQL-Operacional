package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.InventoryResponse;
import com.institutojf.mottainai.model.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return toResponse(inventory, false);
    }

    public InventoryResponse toResponse(Inventory inventory, boolean reactivated) {
        return new InventoryResponse(inventory.getId(), inventory.getStore().getId(), inventory.getBatch().getId(),
                inventory.getInventoryType(), inventory.getCurrentQuantity(), inventory.getMinimumQuantity(),
                inventory.getMaximumQuantity(), inventory.getLocation(), inventory.getVersion(), reactivated);
    }

}
