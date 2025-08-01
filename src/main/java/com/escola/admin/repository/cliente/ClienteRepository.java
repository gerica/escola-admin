package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.Cliente;
import com.escola.admin.model.entity.cliente.StatusCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends CrudRepository<Cliente, Long> {

    @Query("SELECT e FROM Cliente e " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND ( (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(e.docCPF) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.docRG) LIKE LOWER(CONCAT('%', :criteria, '%'))) )")
    Optional<Page<Cliente>> findByFiltro(@Param("criteria") String filtro, @Param("idEmpresa") Long idEmpresa, Pageable pageable);

    @Query("SELECT e FROM Cliente e " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND e.statusCliente = :statusCliente " +
            " AND ( (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(e.docCPF) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.docRG) LIKE LOWER(CONCAT('%', :criteria, '%'))) )")
    Optional<Page<Cliente>> findByStatusClienteAndFiltro(@Param("criteria") String filtro,
                                                         @Param("idEmpresa") Long idEmpresa,
                                                         @Param("statusCliente") StatusCliente statusCliente,
                                                         Pageable pageable);

    // Sua query existente
    @Query("SELECT e FROM Cliente e " +
            " LEFT JOIN FETCH e.dependentes d " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND e.statusCliente = :statusCliente " +
            " AND ( " +
            " (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataNascimento, 'YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(e.docCPF) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.docRG) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR " +
            " (LOWER(d.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(FUNCTION('TO_CHAR', d.dataNascimento, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', d.dataNascimento, 'MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', d.dataNascimento, 'YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(d.docCPF) LIKE LOWER(CONCAT('%', :criteria, '%')) )" +
            ")")
    Optional<Page<Cliente>> findAllClientsByStatusWithDependents(@Param("criteria") String filtro,
                                                                 @Param("idEmpresa") Long idEmpresa,
                                                                 @Param("statusCliente") StatusCliente statusCliente,
                                                                 Pageable pageable);


}