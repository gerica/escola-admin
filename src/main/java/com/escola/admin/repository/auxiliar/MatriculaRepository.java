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
}