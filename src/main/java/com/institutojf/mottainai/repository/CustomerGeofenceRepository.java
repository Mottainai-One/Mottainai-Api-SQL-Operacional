package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.CustomerGeofence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerGeofenceRepository extends JpaRepository<CustomerGeofence, Integer> {

    List<CustomerGeofence> findByCustomer_IdAndActiveTrueOrderByCreatedAtDesc(Integer customerId);

    Optional<CustomerGeofence> findByIdAndCustomer_Id(Integer id, Integer customerId);

    Optional<CustomerGeofence> findByCustomer_IdAndStore_Id(Integer customerId, Integer storeId);

}
