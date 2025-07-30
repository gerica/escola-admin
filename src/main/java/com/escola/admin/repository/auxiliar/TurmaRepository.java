package com.escola.admin.repository.auxiliar;

import com.escola.admin.model.entity.auxiliar.Turma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TurmaRepository extends CrudRepository<Turma, Long> {

    @Query("SELECT e FROM Turma e " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND ( (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.codigo) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.professor) LIKE LOWER(CONCAT('%', :criteria, '%'))) ) ")
    Optional<Page<Turma>> findByFiltro(@Param("criteria") String filtro, @Param("idEmpresa") Long idEmpresa, Pageable pageable);
}