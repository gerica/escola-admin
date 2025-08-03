package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.Anexo;
import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    @Query(value = "SELECT e FROM Anexo e " +
            " WHERE e.contrato.id = :idContrato ")
    @QueryHints(value = {
            @QueryHint(name = "javax.persistence.query.timeout", value = "5000"),
            @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "USE"), //USE, BYPASS, REFRESH
            @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "USE"),
            @QueryHint(name = "org.hibernate.comment", value = "Recuperar contratos utilizando um filtro.\"")
    })
    Optional<List<Anexo>> findByIdContrato(@Param("idContrato") Long idCntrato);

//    @Modifying
//    @Transactional
//    @Query("DELETE FROM Anexo e " +
//            " WHERE e.contrato.id = :idContrato ")
//    void deleteByAnexoById(@Param("idContrato") Long idContrato);

}