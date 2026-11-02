package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.model.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {

    /**
     * Busca uma categoria disponível para uso operacional pelo identificador. Categorias
     * inativas ou excluídas logicamente não são retornadas.
     */
    Optional<ProductCategory> findByIdAndActiveTrueAndDeletedAtIsNull(Integer id);

    Optional<ProductCategory> findByIdAndDeletedAtIsNull(Integer id);

    /**
     * Lista apenas categorias disponíveis para uso operacional, de forma paginada.
     */
    Page<ProductCategory> findAllByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    /**
     * Busca uma categoria pelo nome, ignorando maiúsculas e minúsculas. Usado para
     * validar duplicidade durante criação e atualização.
     */
    Optional<ProductCategory> findByNameIgnoreCase(String name);

    /**
     * Verifica se já existe uma categoria com o nome informado, ignorando maiúsculas e
     * minúsculas. A validação antecipada permite retornar um erro amigável; o banco
     * mantém a restrição final de unicidade.
     */
    boolean existsByNameIgnoreCase(String name);

}
