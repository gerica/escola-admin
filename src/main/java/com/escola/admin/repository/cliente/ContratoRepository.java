package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.entity.cliente.StatusContrato;
import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    @Query(value = "SELECT e FROM Contrato e " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND (:status IS NULL OR e.statusContrato in :status) " + // <-- This line was added
            " AND ( (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.numeroContrato) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.cliente.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(FUNCTION('TO_CHAR', e.dataInicio, 'DD/MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataInicio, 'MM/YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR" +
            " (LOWER(FUNCTION('TO_CHAR', e.dataInicio, 'YYYY')) LIKE LOWER(CONCAT('%', :criteria, '%'))) )"
    )
    @QueryHints(value = {
            @QueryHint(name = "javax.persistence.query.timeout", value = "5000"),
            @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "USE"), //USE, BYPASS, REFRESH
            @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "USE"),
            @QueryHint(name = "org.hibernate.comment", value = "Recuperar contratos utilizando um filtro e status opcional.")
    })
    Optional<Page<Contrato>> findByFiltro(@Param("criteria") String filtro,
                                          @Param("idEmpresa") Long idEmpresa,
                                          @Param("status") List<StatusContrato> status, // <-- @Param added
                                          Pageable pageable);

    @Query(value = "SELECT e FROM Contrato e " +
            " WHERE e.matricula.id = :idMatricula ")
    Optional<Contrato> findByIdMatricula(@Param("idMatricula") Long idMatricula);

    @Modifying
    @Transactional
    @Query("DELETE FROM Contrato c WHERE c.matricula.id = :idMatricula")
    void deleteByMatriculaId(@Param("idMatricula") Long idMatricula);

    /**
     * Busca diretamente o valor da mensalidade do curso associado a um contrato,
     * usando uma única query otimizada sem carregar as entidades completas.
     *
     * @param idContrato O ID do contrato.
     * @return Um Optional contendo o valor da mensalidade, se encontrado.
     */
    @Query("SELECT cr.valorMensalidade " +
            "FROM Contrato c " +
            "JOIN c.matricula m " +
            "JOIN m.turma t " +
            "JOIN t.curso cr " +
            "WHERE c.id = :idContrato")
    Optional<BigDecimal> findValorMensalidadeByContratoId(@Param("idContrato") Long idContrato);
}