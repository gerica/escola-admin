package com.escola.admin.repository.auxiliar;

import com.escola.admin.model.entity.auxiliar.Cargo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CargoRepository extends CrudRepository<Cargo, Long> {

    @Query("SELECT e FROM Cargo e " +
            " WHERE (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.nome) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.descricao) LIKE LOWER(CONCAT('%', :criteria, '%')) ) ")
    Optional<Page<Cargo>> findByFiltro(@Param("criteria") String filtro, Pageable pageable);

}