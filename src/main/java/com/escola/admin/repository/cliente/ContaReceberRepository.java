package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.entity.cliente.StatusContrato;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {

    @Query(value = "SELECT e FROM ContaReceber e " +
            " WHERE e.contrato.id = :idContrato " +
            " ORDER BY e.dataVencimento asc")
    @QueryHints(value = {
            @QueryHint(name = "javax.persistence.query.timeout", value = "5000"),
            @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "USE"), //USE, BYPASS, REFRESH
            @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "USE"),
            @QueryHint(name = "org.hibernate.comment", value = "Recuperar contratos utilizando um filtro.\"")
    })
    Optional<List<ContaReceber>> findByIdContrato(@Param("idContrato") Long idCntrato);

    @Query(value = "SELECT cr FROM ContaReceber cr " +
            "JOIN FETCH cr.contrato c " +
            "WHERE cr.dataVencimento BETWEEN :inicioDoMes AND :fimDoMes " +
            "AND c.statusContrato IN :statusContrato")
    @QueryHints(value = {
            @QueryHint(name = "javax.persistence.query.timeout", value = "5000"),
            @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "USE"),
            @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "USE"),
            @QueryHint(name = "org.hibernate.comment", value = "Recupera todas as contas a receber de contratos ativos em um determinado mês e ano.")
    })
    Optional<List<ContaReceber>> findByMesAndContratoStatus(
            @Param("inicioDoMes") java.time.LocalDate inicioDoMes,
            @Param("fimDoMes") java.time.LocalDate fimDoMes,
            @Param("statusContrato") Collection<StatusContrato> statusContratos
    );

    @Query(value = "SELECT cr FROM ContaReceber cr " +
            "JOIN FETCH cr.contrato c " +
            "WHERE cr.dataVencimento BETWEEN :inicioDoMes AND :fimDoMes " +
            "AND c.statusContrato IN :statusContrato")
    @QueryHints(value = {
            @QueryHint(name = "javax.persistence.query.timeout", value = "5000"),
            @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "USE"),
            @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "USE"),
            @QueryHint(name = "org.hibernate.comment", value = "Recupera todas as contas a receber de contratos ativos em um determinado mês e ano.")
    })
    Optional<Page<ContaReceber>> findByDataRef(
            @Param("inicioDoMes") java.time.LocalDate inicioDoMes,
            @Param("fimDoMes") java.time.LocalDate fimDoMes,
            @Param("statusContrato") Collection<StatusContrato> statusContratos,
            Pageable effectivePageable);
}