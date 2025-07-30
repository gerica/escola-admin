package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.ClienteDependente;
import com.escola.admin.model.request.cliente.ClienteDependenteRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ClienteDependenteService {

    Mono<ClienteDependente> save(ClienteDependenteRequest request);

    Optional<Boolean> apagar(Long id);

    Mono<ClienteDependente> findById(Long id);

    Optional<List<ClienteDependente>> findAllByClienteId(Long id);


}
