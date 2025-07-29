package com.escola.admin.repository.auxiliar;

import com.escola.admin.model.entity.auxiliar.Curso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CursoRepository extends CrudRepository<Curso, Long> {

    @Query("SELECT e FROM Curso e " +
            " WHERE (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.categoria) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.descricao) LIKE LOWER(CONCAT('%', :criteria, '%')) ) ")
    Optional<Page<Curso>> findByFiltro(@Param("criteria") String filtro, Pageable pageable);

    @Query("SELECT e FROM Curso e " +
            " WHERE e.empresa.id = :idEmpresa " +
            " AND ( (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.categoria) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.descricao) LIKE LOWER(CONCAT('%', :criteria, '%'))) ) ")
    Optional<Page<Curso>> findByFiltro(@Param("criteria") String filtro, @Param("idEmpresa") Long idEmpresa, @Param("pageable") Pageable pageable);


}