package com.escola.admin.repository.cliente;

import com.escola.admin.model.entity.cliente.ClienteContato;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteContatoRepository extends CrudRepository<ClienteContato, Long> {

    @Query("SELECT e FROM ClienteContato e " +
            " WHERE e.cliente.id = :id")
    Optional<List<ClienteContato>> findAllByClienteId(@Param("id") Long id);
}