package com.escola.admin.repository;

import com.escola.admin.model.entity.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmpresaRepository extends CrudRepository<Empresa, Long> {

    @Query("SELECT e FROM Empresa e " +
            " WHERE (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nomeFantasia) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.razaoSocial) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.email) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.cnpj) LIKE LOWER(CONCAT('%', :criteria, '%')) ) ")
    Optional<Page<Empresa>> findByFiltro(@Param("criteria") String filtro, Pageable pageable);

}