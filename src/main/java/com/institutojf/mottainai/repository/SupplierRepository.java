package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    /**
     * Busca um fornecedor disponível para uso operacional pelo identificador.
     * Fornecedores inativos ou excluídos logicamente não são retornados.
     */
    Optional<Supplier> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<Supplier> findByIdAndDeletedAtIsNull(Integer id);

    /**
     * Lista apenas fornecedores disponíveis para uso operacional, de forma paginada.
     */
    Page<Supplier> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    /**
     * Verifica se o CNPJ já foi cadastrado. A consulta considera inclusive fornecedores
     * inativos, pois o banco exige CNPJ único globalmente.
     */
    boolean existsByCnpj(String cnpj);

}
