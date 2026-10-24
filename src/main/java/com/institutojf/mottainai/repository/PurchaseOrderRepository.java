package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.dto.request.CreatePurchaseOrderItemRequest;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends Repository<AppUser, Integer> {

    @Query(value = "SELECT mottainai.fn_api_insert_purchase_order(:storeId, :supplierId, :employeeId, :expectedDeliveryDate, :observation, :total)", nativeQuery = true)
    Integer insertOrder(@Param("storeId") Integer storeId, @Param("supplierId") Integer supplierId, @Param("employeeId") Integer employeeId, @Param("expectedDeliveryDate") LocalDate expectedDeliveryDate, @Param("observation") String observation, @Param("total") BigDecimal total);

    @Modifying
    @Query(value = "INSERT INTO mottainai.purchase_order_item (purchase_order_id, product_id, requested_quantity, unit_price, order_date) VALUES (:orderId, :productId, :quantity, :unitCost, :orderDate)", nativeQuery = true)
    int insertItem(@Param("orderId") Integer orderId, @Param("productId") Integer productId, @Param("quantity") BigDecimal quantity, @Param("unitCost") BigDecimal unitCost, @Param("orderDate") LocalDateTime orderDate);

    @Query(value = "SELECT * FROM mottainai.vw_api_purchase_order WHERE purchase_order_id = :id AND deleted_at IS NULL", nativeQuery = true)
    Optional<PurchaseOrderProjection> queryById(@Param("id") Integer id);

    @Query(value = "SELECT * FROM mottainai.vw_api_purchase_order WHERE purchase_order_id = :id AND deleted_at IS NULL FOR UPDATE", nativeQuery = true)
    Optional<PurchaseOrderProjection> queryByIdForUpdate(@Param("id") Integer id);

    @Query(value = "SELECT * FROM mottainai.vw_api_purchase_order WHERE store_id = :storeId AND deleted_at IS NULL AND order_date BETWEEN :from AND :to ORDER BY order_date DESC", nativeQuery = true)
    List<PurchaseOrderProjection> queryAll(@Param("storeId") Integer storeId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT * FROM mottainai.vw_api_purchase_order_item WHERE purchase_order_id = :orderId AND order_date = :orderDate AND deleted_at IS NULL ORDER BY purchase_order_item_id", nativeQuery = true)
    List<PurchaseOrderItemProjection> queryItems(@Param("orderId") Integer orderId, @Param("orderDate") LocalDateTime orderDate);

    @Modifying
    @Query(value = "UPDATE mottainai.purchase_order SET supplier_id = :supplierId, expected_delivery_date = :expectedDeliveryDate, observation = :observation, total_amount = :total, updated_at = NOW(), version = version + 1 WHERE purchase_order_id = :id AND order_date = :orderDate AND version = :version AND status = 'PENDING' AND deleted_at IS NULL", nativeQuery = true)
    int updateOrder(@Param("id") Integer id, @Param("orderDate") LocalDateTime orderDate, @Param("supplierId") Integer supplierId, @Param("expectedDeliveryDate") LocalDate expectedDeliveryDate, @Param("observation") String observation, @Param("total") BigDecimal total, @Param("version") Integer version);

    @Modifying
    @Query(value = "UPDATE mottainai.purchase_order_item SET deleted_at = NOW() WHERE purchase_order_id = :orderId AND order_date = :orderDate AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteItems(@Param("orderId") Integer orderId, @Param("orderDate") LocalDateTime orderDate);

    @Modifying
    @Query(value = "UPDATE mottainai.purchase_order SET status = CAST(:status AS mottainai.purchase_order_status), updated_at = NOW(), version = version + 1 WHERE purchase_order_id = :id AND order_date = :orderDate AND version = :version AND deleted_at IS NULL", nativeQuery = true)
    int updateOrderStatus(@Param("id") Integer id, @Param("orderDate") LocalDateTime orderDate, @Param("version") Integer version, @Param("status") String status);

    @Modifying
    @Query(value = "UPDATE mottainai.purchase_order SET deleted_at = NOW(), updated_at = NOW(), version = version + 1 WHERE purchase_order_id = :id AND order_date = :orderDate AND version = :version AND status = 'PENDING' AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteOrder(@Param("id") Integer id, @Param("orderDate") LocalDateTime orderDate, @Param("version") Integer version);

    default PurchaseOrderData insert(Integer storeId, Integer supplierId, Integer employeeId, LocalDate expectedDeliveryDate, String observation, List<CreatePurchaseOrderItemRequest> items) {
        Integer id = insertOrder(storeId, supplierId, employeeId, expectedDeliveryDate, observation, total(items));
        PurchaseOrderData order = findById(id).orElseThrow();
        insertItems(id, order.orderDate(), items);
        return findById(id).orElseThrow();
    }

    default Optional<PurchaseOrderData> findById(Integer id) {
        return queryById(id).map(this::withItems);
    }

    default Optional<PurchaseOrderData> findByIdForUpdate(Integer id) {
        return queryByIdForUpdate(id).map(this::withItems);
    }

    default List<PurchaseOrderData> findAll(Integer storeId, LocalDateTime from, LocalDateTime to) {
        return queryAll(storeId, from, to).stream().map(this::withItems).toList();
    }

    default boolean update(PurchaseOrderData current, Integer supplierId, LocalDate expectedDeliveryDate, String observation, List<CreatePurchaseOrderItemRequest> items, Integer version) {
        if (updateOrder(current.id(), current.orderDate(), supplierId, expectedDeliveryDate, observation, total(items), version) != 1) {
            return false;
        }
        softDeleteItems(current.id(), current.orderDate());
        insertItems(current.id(), current.orderDate(), items);
        return true;
    }

    default boolean updateStatus(PurchaseOrderData current, PurchaseOrderStatus status) {
        return updateOrderStatus(current.id(), current.orderDate(), current.version(), status.name()) == 1;
    }

    default boolean softDelete(PurchaseOrderData current) {
        return softDeleteOrder(current.id(), current.orderDate(), current.version()) == 1;
    }

    private void insertItems(Integer orderId, LocalDateTime orderDate, List<CreatePurchaseOrderItemRequest> items) {
        items.forEach(item -> insertItem(orderId, item.productId(), item.requestedQuantity(), item.unitCost(), orderDate));
    }

    private PurchaseOrderData withItems(PurchaseOrderProjection order) {
        return order.toData().withItems(queryItems(order.getPurchaseOrderId(), order.getOrderDate()).stream().map(PurchaseOrderItemProjection::toData).toList());
    }

    private BigDecimal total(List<CreatePurchaseOrderItemRequest> items) {
        return items.stream().map(item -> item.requestedQuantity().multiply(item.unitCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    interface PurchaseOrderProjection {
        Integer getPurchaseOrderId();
        Integer getStoreId();
        Integer getSupplierId();
        Integer getEmployeeId();
        LocalDateTime getOrderDate();
        LocalDate getExpectedDeliveryDate();
        String getStatus();
        String getObservation();
        BigDecimal getTotalAmount();
        Integer getVersion();

        default PurchaseOrderData toData() {
            return new PurchaseOrderData(getPurchaseOrderId(), getStoreId(), getSupplierId(), getEmployeeId(), getOrderDate(), getExpectedDeliveryDate(), PurchaseOrderStatus.valueOf(getStatus()), getObservation(), getTotalAmount(), getVersion(), List.of());
        }
    }

    interface PurchaseOrderItemProjection {
        Integer getPurchaseOrderItemId();
        Integer getProductId();
        BigDecimal getRequestedQuantity();
        BigDecimal getUnitPrice();
        BigDecimal getSubtotal();

        default PurchaseOrderItemData toData() {
            return new PurchaseOrderItemData(getPurchaseOrderItemId(), getProductId(), getRequestedQuantity(), getUnitPrice(), getSubtotal());
        }
    }

    record PurchaseOrderData(Integer id, Integer storeId, Integer supplierId, Integer employeeId, LocalDateTime orderDate, LocalDate expectedDeliveryDate, PurchaseOrderStatus status, String observation, BigDecimal totalAmount, Integer version, List<PurchaseOrderItemData> items) {
        public PurchaseOrderData withItems(List<PurchaseOrderItemData> value) {
            return new PurchaseOrderData(id, storeId, supplierId, employeeId, orderDate, expectedDeliveryDate, status, observation, totalAmount, version, value);
        }
    }

    record PurchaseOrderItemData(Integer id, Integer productId, BigDecimal requestedQuantity, BigDecimal unitCost, BigDecimal subtotal) {
    }
}
