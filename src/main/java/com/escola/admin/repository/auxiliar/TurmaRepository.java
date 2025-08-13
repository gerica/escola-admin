package com.escola.admin.repository.auxiliar;

import com.escola.admin.model.entity.auxiliar.Curso;
import com.escola.admin.model.entity.auxiliar.StatusTurma;
import com.escola.admin.model.entity.auxiliar.Turma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TurmaRepository extends CrudRepository<Turma, Long> {

    @Query("SELECT e FROM Turma e " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND (:status IS NULL OR e.status in :status) " + // <-- This line was added
            " AND ( (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.codigo) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.professor) LIKE LOWER(CONCAT('%', :criteria, '%'))) ) ")
    Optional<Page<Turma>> findByFiltro(@Param("criteria") String filtro,
                                       @Param("status") List<StatusTurma> status,
                                       @Param("idEmpresa") Long idEmpresa,
                                       Pageable pageable);

    @Query("SELECT t FROM Turma t " +
            " JOIN FETCH t.curso c" +
            " JOIN FETCH t.empresa e" +
            " where t.id = :id")
    Optional<Turma> findByIdAndLoadCursoAndEmpresa(@Param("id") Long id);

    @Query("SELECT COUNT(t) FROM Turma t " +
            "WHERE t.curso = :curso " +
            "AND t.anoPeriodo = :anoPeriodo " +
            "AND (LOWER(FUNCTION('TO_CHAR', t.dataInicio, 'YYYY')) LIKE LOWER(CONCAT('%', :ano, '%')))")
    Long countByCursoAndAnoPeriodoAndAno(@Param("curso") Curso curso, @Param("anoPeriodo") String anoPeriodo, @Param("ano") int year);
}