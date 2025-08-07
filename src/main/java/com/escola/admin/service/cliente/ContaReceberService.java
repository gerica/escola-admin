package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.request.cliente.ContaReceberRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ContaReceberService {

    Mono<ContaReceber> save(ContaReceberRequest request);

    Mono<ContaReceber> findById(Long id);

    Mono<List<ContaReceber>> findByFiltro(Long idContrato);

    Mono<Void> criar(Long id);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(ContaReceber entity);


}
