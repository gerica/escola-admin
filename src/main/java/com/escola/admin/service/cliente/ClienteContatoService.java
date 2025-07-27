package com.escola.admin.service.cliente;


import com.escola.admin.model.entity.cliente.ClienteContato;
import com.escola.admin.model.request.cliente.ClienteContatoRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ClienteContatoService {

    Mono<ClienteContato> save(ClienteContatoRequest request);

    Optional<Boolean> apagar(Integer id);

    Optional<ClienteContato> findById(Integer id);

    Optional<List<ClienteContato>> findAllByClienteId(Integer id);


}
