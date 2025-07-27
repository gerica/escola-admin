package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends CrudRepository<Cliente, Integer> {

    @Query("SELECT e FROM Cliente e " +
            " WHERE (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(e.docCPF) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.docRG) LIKE LOWER(CONCAT('%', :criteria, '%')) ) ")
    Optional<Page<Cliente>> findByFiltro(@Param("criteria") String filtro, Pageable pageable);
}