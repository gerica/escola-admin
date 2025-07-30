package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.ClienteDependente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteDependenteRepository extends CrudRepository<ClienteDependente, Long> {

    @Query("SELECT e FROM ClienteDependente e " +
            " WHERE e.cliente.id = :id")
    Optional<List<ClienteDependente>> findAllByClienteId(Long id);
}