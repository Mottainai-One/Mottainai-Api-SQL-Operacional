package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.CustomerGeofence;

import java.time.LocalDateTime;

public record CustomerGeofenceResponse(
        Integer id,
        Integer customerId,
        Integer storeId,
        String storeName,
        Integer radiusMeters,
        Boolean active,
        LocalDateTime createdAt
) {
    public static CustomerGeofenceResponse fromEntity(CustomerGeofence geofence) {
        return new CustomerGeofenceResponse(geofence.getId(), geofence.getCustomer().getId(),
                geofence.getStore().getId(), geofence.getStore().getName(), geofence.getRadiusMeters(),
                geofence.getActive(), geofence.getCreatedAt());
    }
}
