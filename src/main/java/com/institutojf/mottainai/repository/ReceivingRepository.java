package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.dto.request.CreateReceivingItemRequest;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.enums.ReceivingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReceivingRepository extends Repository<AppUser, Integer> {

    @Query(value = "SELECT EXISTS (SELECT 1 FROM mottainai.receiving WHERE purchase_order_id = :orderId AND order_date = :orderDate)", nativeQuery = true)
    boolean existsForPurchaseOrder(@Param("orderId") Integer orderId, @Param("orderDate") LocalDateTime orderDate);

    @Query(value = "SELECT mottainai.fn_api_insert_receiving(:orderId, :employeeId, :observation, :orderDate)", nativeQuery = true)
    Integer insertReceiving(@Param("orderId") Integer orderId, @Param("employeeId") Integer employeeId, @Param("observation") String observation, @Param("orderDate") LocalDateTime orderDate);

    @Modifying
    @Query(value = "INSERT INTO mottainai.receiving_item (receiving_id, purchase_order_item_id, received_quantity, unit_price, manufacture_date, expiration_date, observation) VALUES (:receivingId, :orderItemId, :quantity, :unitCost, :manufactureDate, :expirationDate, :observation)", nativeQuery = true)
    int insertItem(@Param("receivingId") Integer receivingId, @Param("orderItemId") Integer orderItemId, @Param("quantity") BigDecimal quantity, @Param("unitCost") BigDecimal unitCost, @Param("manufactureDate") LocalDate manufactureDate, @Param("expirationDate") LocalDate expirationDate, @Param("observation") String observation);

    @Query(value = "SELECT * FROM mottainai.vw_api_receiving WHERE receiving_id = :id", nativeQuery = true)
    Optional<ReceivingProjection> queryById(@Param("id") Integer id);

    @Query(value = "SELECT * FROM mottainai.vw_api_receiving WHERE receiving_id = :id FOR UPDATE", nativeQuery = true)
    Optional<ReceivingProjection> queryByIdForUpdate(@Param("id") Integer id);

    @Query(value = "SELECT * FROM mottainai.vw_api_receiving WHERE store_id = :storeId AND receiving_date BETWEEN :from AND :to AND purchase_order_deleted_at IS NULL ORDER BY receiving_date DESC", nativeQuery = true)
    List<ReceivingProjection> queryAll(@Param("storeId") Integer storeId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT * FROM mottainai.vw_api_receiving_item WHERE receiving_id = :receivingId AND deleted_at IS NULL ORDER BY receiving_item_id", nativeQuery = true)
    List<ReceivingItemProjection> findItems(@Param("receivingId") Integer receivingId);

    @Modifying
    @Query(value = "UPDATE mottainai.receiving SET status = CAST(:status AS mottainai.receiving_status), updated_at = NOW() WHERE receiving_id = :id AND status = 'PENDING'", nativeQuery = true)
    int updateReceivingStatus(@Param("id") Integer id, @Param("status") String status);

    @Query(value = "SELECT b.batch_id AS batchId, ri.received_quantity AS quantity FROM mottainai.receiving_item ri JOIN mottainai.batch b ON b.receiving_item_id = ri.receiving_item_id WHERE ri.receiving_id = :receivingId ORDER BY ri.receiving_item_id", nativeQuery = true)
    List<BatchCreditProjection> findBatchCredits(@Param("receivingId") Integer receivingId);

    @Query(value = "SELECT mottainai.fn_api_upsert_inventory(:storeId, :batchId)", nativeQuery = true)
    Integer upsertInventory(@Param("storeId") Integer storeId, @Param("batchId") Integer batchId);

    @Query(value = "SELECT mottainai.fn_atomic_update_inventory(:inventoryId, :quantity, 'IN', :employeeId, :reason, NULL)", nativeQuery = true)
    BigDecimal creditInventoryQuantity(@Param("inventoryId") Integer inventoryId, @Param("quantity") BigDecimal quantity, @Param("employeeId") Integer employeeId, @Param("reason") String reason);

    default ReceivingData insert(PurchaseOrderRepository.PurchaseOrderData order, Integer employeeId, String observation, List<CreateReceivingItemRequest> items) {
        Integer id = insertReceiving(order.id(), employeeId, observation, order.orderDate());
        items.forEach(item -> insertItem(id, item.purchaseOrderItemId(), item.receivedQuantity(), item.unitCost(), item.manufactureDate(), item.expirationDate(), item.observation()));
        return findById(id).orElseThrow();
    }

    default Optional<ReceivingData> findById(Integer id) {
        return queryById(id).map(this::withItems);
    }

    default Optional<ReceivingData> findByIdForUpdate(Integer id) {
        return queryByIdForUpdate(id).map(this::withItems);
    }

    default List<ReceivingData> findAll(Integer storeId, LocalDateTime from, LocalDateTime to) {
        return queryAll(storeId, from, to).stream().map(this::withItems).toList();
    }

    default void updateStatus(Integer id, ReceivingStatus status) {
        updateReceivingStatus(id, status.name());
    }

    default void creditInventory(Integer receivingId, Integer storeId, Integer employeeId) {
        findBatchCredits(receivingId).forEach(credit -> {
            Integer inventoryId = upsertInventory(storeId, credit.getBatchId());
            creditInventoryQuantity(inventoryId, credit.getQuantity(), employeeId, "Purchase order receiving " + receivingId);
        });
    }

    private ReceivingData withItems(ReceivingProjection receiving) {
        List<ReceivingItemData> items = findItems(receiving.getReceivingId()).stream().map(ReceivingItemProjection::toData).toList();
        return receiving.toData().withItems(items);
    }

    interface ReceivingProjection {
        Integer getReceivingId();
        Integer getPurchaseOrderId();
        Integer getStoreId();
        Integer getEmployeeId();
        LocalDateTime getReceivingDate();
        String getStatus();
        String getObservation();
        LocalDateTime getOrderDate();

        default ReceivingData toData() {
            return new ReceivingData(getReceivingId(), getPurchaseOrderId(), getStoreId(), getEmployeeId(), getReceivingDate(), ReceivingStatus.valueOf(getStatus()), getObservation(), getOrderDate(), List.of());
        }
    }

    interface ReceivingItemProjection {
        Integer getReceivingItemId();
        Integer getPurchaseOrderItemId();
        Integer getProductId();
        BigDecimal getRequestedQuantity();
        BigDecimal getReceivedQuantity();
        BigDecimal getUnitPrice();
        LocalDate getManufactureDate();
        LocalDate getExpirationDate();
        String getObservation();

        default ReceivingItemData toData() {
            return new ReceivingItemData(getReceivingItemId(), getPurchaseOrderItemId(), getProductId(), getRequestedQuantity(), getReceivedQuantity(), getUnitPrice(), getManufactureDate(), getExpirationDate(), getObservation());
        }
    }

    interface BatchCreditProjection {
        Integer getBatchId();
        BigDecimal getQuantity();
    }

    record ReceivingData(Integer id, Integer purchaseOrderId, Integer storeId, Integer employeeId, LocalDateTime receivingDate, ReceivingStatus status, String observation, LocalDateTime orderDate, List<ReceivingItemData> items) {
        public ReceivingData withItems(List<ReceivingItemData> value) {
            return new ReceivingData(id, purchaseOrderId, storeId, employeeId, receivingDate, status, observation, orderDate, value);
        }
    }

    record ReceivingItemData(Integer id, Integer purchaseOrderItemId, Integer productId, BigDecimal requestedQuantity, BigDecimal receivedQuantity, BigDecimal unitCost, LocalDate manufactureDate, LocalDate expirationDate, String observation) {
    }
}
