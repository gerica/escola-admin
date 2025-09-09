package com.escola.admin.repository.auxiliar;

import com.escola.admin.model.entity.auxiliar.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatriculaRepository extends CrudRepository<Matricula, Long> {

    @Query("SELECT e FROM Matricula e " +
            " WHERE e.turma.id = :idTurma ")
    Optional<Page<Matricula>> findByIdTurma(@Param("idTurma") Long idTurma, Pageable pageable);

    @Query("SELECT m FROM Matricula m " +
            " LEFT JOIN FETCH m.turma t " +
            " LEFT JOIN FETCH m.cliente c " +
            " LEFT JOIN FETCH m.clienteDependente cd " +
            " WHERE m.id = :id ")
    Optional<Matricula> findByIdWithClienteAndDependente(@Param("id") Long id);

//    @Query("SELECT m FROM Matricula m " +
//            "LEFT JOIN FETCH m.turma t " +
//            "LEFT JOIN FETCH m.cliente c " +
//            "WHERE m.id = :id")
//    Optional<Matricula> findByIdWithTurmaAndCliente(@Param("id") Long id);

    @Query("SELECT m FROM Matricula m WHERE m.turma.id = :idTurma ORDER BY m.codigo DESC LIMIT 1")
    Optional<Matricula> findTopByTurmaIdOrderByCodigoDesc(@Param("idTurma") Long idTurma);

    @Query("SELECT m FROM Matricula m " +
            "LEFT JOIN FETCH m.turma t " +
            "WHERE m.codigo = :codigo")
    Optional<Matricula> findByCodigo(@Param("codigo") String codigo);
}