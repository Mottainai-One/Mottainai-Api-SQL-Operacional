package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    /**
     * Busca um produto ativo pelo ID.
     */
    Optional<Product> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<Product> findByIdAndDeletedAtIsNull(Integer id);

    /**
     * Busca um produto disponível para uso operacional pelo código de barras.
     */
    Optional<Product> findByBarcodeAndActiveTrueAndDeletedAtIsNull(String barcode);

    /**
     * Lista apenas produtos disponíveis para uso operacional, de forma paginada.
     */
    Page<Product> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    boolean existsByCompany_IdAndBarcode(Integer companyId, String barcode);

    boolean existsByCategory_IdAndActiveTrueAndDeletedAtIsNull(Integer categoryId);

    boolean existsByTaxProfile_IdAndActiveTrueAndDeletedAtIsNull(Integer taxProfileId);
}
