package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.SupplierProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Integer> {

    /**
     * Busca um vínculo fornecedor-produto disponível para uso operacional. Vínculos
     * inativos ou excluídos logicamente não são retornados.
     */
    Optional<SupplierProduct> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<SupplierProduct> findByIdAndDeletedAtIsNull(Integer id);

    Optional<SupplierProduct> findBySupplier_IdAndProduct_IdAndDeletedAtIsNull(Integer supplierId, Integer productId);

    Page<SupplierProduct> findAllByProduct_IdAndActiveTrueAndDeletedAtIsNull(Integer productId, Pageable pageable);

    Page<SupplierProduct> findAllBySupplier_IdAndActiveTrueAndDeletedAtIsNull(Integer supplierId, Pageable pageable);

    /**
     * Lista os vínculos ativos entre fornecedores e produtos, de forma paginada.
     */
    Page<SupplierProduct> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    /**
     * Verifica se o fornecedor já fornece o produto informado. Evita duplicidade antes de
     * persistir; o banco também protege a relação por UNIQUE (supplier_id, product_id).
     */
    boolean existsBySupplier_IdAndProduct_Id(Integer supplierId, Integer productId);

    boolean existsBySupplier_IdAndActiveTrueAndDeletedAtIsNull(Integer supplierId);

    boolean existsByProduct_IdAndActiveTrueAndDeletedAtIsNull(Integer productId);

}
