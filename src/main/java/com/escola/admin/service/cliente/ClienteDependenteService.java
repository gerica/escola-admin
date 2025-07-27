package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.request.cliente.ClienteDependenteRequest;

import java.util.List;
import java.util.Optional;

public interface ClienteDependenteService {

    ClienteDependente save(ClienteDependenteRequest request);

    Optional<Boolean> apagar(Integer id);

    Optional<ClienteDependente> findById(Integer id);

    Optional<List<ClienteDependente>> findAllByClienteId(Integer id);


}
