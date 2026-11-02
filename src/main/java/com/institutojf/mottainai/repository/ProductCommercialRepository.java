package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.dto.response.ProductHistoryResponse;
import com.institutojf.mottainai.dto.response.ProductPriceHistoryResponse;
import com.institutojf.mottainai.dto.response.StoreProductPriceResponse;
import com.institutojf.mottainai.dto.response.SupplierPurchaseHistoryResponse;
import com.institutojf.mottainai.model.AppUser;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductCommercialRepository extends Repository<AppUser, Integer> {

    @Query(value = "SELECT store_product_price_id, store_id, product_id, regular_price, valid_from, valid_until, active, version FROM mottainai.vw_api_store_product_price WHERE product_id = :productId AND active = TRUE AND valid_until IS NULL ORDER BY store_id", nativeQuery = true)
    List<StorePriceProjection> queryStorePrices(@Param("productId") Integer productId);

    @Query(value = "SELECT store_product_price_id, store_id, product_id, regular_price, valid_from, valid_until, active, version FROM mottainai.vw_api_store_product_price WHERE product_id = :productId AND store_id = :storeId AND active = TRUE AND valid_until IS NULL FOR UPDATE", nativeQuery = true)
    Optional<StorePriceProjection> queryStorePriceForUpdate(@Param("productId") Integer productId, @Param("storeId") Integer storeId);

    @Modifying
    @Query(value = "UPDATE mottainai.store_product_price SET regular_price = :price, updated_at = NOW(), version = version + 1 WHERE store_product_price_id = :id AND version = :version", nativeQuery = true)
    int updateStorePrice(@Param("id") Long id, @Param("version") Integer version, @Param("price") BigDecimal price);

    @Modifying
    @Query(value = "INSERT INTO mottainai.store_product_price (store_id, product_id, regular_price) VALUES (:storeId, :productId, :price)", nativeQuery = true)
    int insertStorePrice(@Param("productId") Integer productId, @Param("storeId") Integer storeId, @Param("price") BigDecimal price);

    @Modifying
    @Query(value = "INSERT INTO mottainai.product_price_history (product_id, old_price, new_price, changed_by) VALUES (:productId, :oldPrice, :newPrice, :changedBy)", nativeQuery = true)
    int insertPriceHistory(@Param("productId") Integer productId, @Param("oldPrice") BigDecimal oldPrice, @Param("newPrice") BigDecimal newPrice, @Param("changedBy") Integer changedBy);

    @Query(value = "SELECT history_id, product_id, field_name, old_value, new_value, changed_by, changed_at FROM mottainai.vw_api_product_history WHERE product_id = :productId AND changed_at BETWEEN :from AND :to ORDER BY changed_at DESC, history_id DESC", nativeQuery = true)
    List<ProductHistoryProjection> queryMasterHistory(@Param("productId") Integer productId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT price_history_id, product_id, old_price, new_price, changed_by, changed_at FROM mottainai.vw_api_product_price_history WHERE product_id = :productId AND changed_at BETWEEN :from AND :to ORDER BY changed_at DESC, price_history_id DESC", nativeQuery = true)
    List<ProductPriceHistoryProjection> queryPriceHistory(@Param("productId") Integer productId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT purchase_order_id, store_id, employee_id, order_date, expected_delivery_date, status, total_amount FROM mottainai.vw_api_purchase_order WHERE supplier_id = :supplierId AND deleted_at IS NULL AND order_date BETWEEN :from AND :to ORDER BY order_date DESC", nativeQuery = true)
    List<SupplierHistoryProjection> querySupplierPurchaseHistory(@Param("supplierId") Integer supplierId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    default List<StoreProductPriceResponse> findStorePrices(Integer productId) {
        return queryStorePrices(productId).stream().map(StorePriceProjection::toResponse).toList();
    }

    default Optional<StoreProductPriceResponse> findStorePriceForUpdate(Integer productId, Integer storeId) {
        return queryStorePriceForUpdate(productId, storeId).map(StorePriceProjection::toResponse);
    }

    default StoreProductPriceResponse saveStorePrice(Integer productId, Integer storeId, BigDecimal price, Integer changedBy) {
        Optional<StoreProductPriceResponse> current = findStorePriceForUpdate(productId, storeId);
        if (current.isPresent()) {
            StoreProductPriceResponse old = current.get();
            updateStorePrice(old.id(), old.version(), price);
            insertPriceHistory(productId, old.regularPrice(), price, changedBy);
        }
        else {
            insertStorePrice(productId, storeId, price);
            insertPriceHistory(productId, null, price, changedBy);
        }
        return findStorePriceForUpdate(productId, storeId).orElseThrow();
    }

    default List<ProductHistoryResponse> findMasterHistory(Integer productId, LocalDateTime from, LocalDateTime to) {
        return queryMasterHistory(productId, from, to).stream().map(ProductHistoryProjection::toResponse).toList();
    }

    default List<ProductPriceHistoryResponse> findPriceHistory(Integer productId, LocalDateTime from, LocalDateTime to) {
        return queryPriceHistory(productId, from, to).stream().map(ProductPriceHistoryProjection::toResponse).toList();
    }

    default List<SupplierPurchaseHistoryResponse> findSupplierPurchaseHistory(Integer supplierId, LocalDateTime from, LocalDateTime to) {
        return querySupplierPurchaseHistory(supplierId, from, to).stream().map(SupplierHistoryProjection::toResponse).toList();
    }

    interface StorePriceProjection {
        Long getStoreProductPriceId();
        Integer getStoreId();
        Integer getProductId();
        BigDecimal getRegularPrice();
        LocalDateTime getValidFrom();
        LocalDateTime getValidUntil();
        Boolean getActive();
        Integer getVersion();

        default StoreProductPriceResponse toResponse() {
            return new StoreProductPriceResponse(getStoreProductPriceId(), getStoreId(), getProductId(), getRegularPrice(), getValidFrom(), getValidUntil(), getActive(), getVersion());
        }
    }

    interface ProductHistoryProjection {
        Long getHistoryId();
        Integer getProductId();
        String getFieldName();
        String getOldValue();
        String getNewValue();
        Integer getChangedBy();
        LocalDateTime getChangedAt();

        default ProductHistoryResponse toResponse() {
            return new ProductHistoryResponse(getHistoryId(), getProductId(), getFieldName(), getOldValue(), getNewValue(), getChangedBy(), getChangedAt());
        }
    }

    interface ProductPriceHistoryProjection {
        Long getPriceHistoryId();
        Integer getProductId();
        BigDecimal getOldPrice();
        BigDecimal getNewPrice();
        Integer getChangedBy();
        LocalDateTime getChangedAt();

        default ProductPriceHistoryResponse toResponse() {
            return new ProductPriceHistoryResponse(getPriceHistoryId(), getProductId(), getOldPrice(), getNewPrice(), getChangedBy(), getChangedAt());
        }
    }

    interface SupplierHistoryProjection {
        Integer getPurchaseOrderId();
        Integer getStoreId();
        Integer getEmployeeId();
        LocalDateTime getOrderDate();
        LocalDate getExpectedDeliveryDate();
        String getStatus();
        BigDecimal getTotalAmount();

        default SupplierPurchaseHistoryResponse toResponse() {
            return new SupplierPurchaseHistoryResponse(getPurchaseOrderId(), getStoreId(), getEmployeeId(), getOrderDate(), getExpectedDeliveryDate(), getStatus(), getTotalAmount());
        }
    }
}
