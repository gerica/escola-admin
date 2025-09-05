package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.request.cliente.ContaReceberRequest;
import com.escola.admin.model.response.cliente.ContaReceberPorMesResumeResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface ContaReceberService {

    Mono<ContaReceber> save(ContaReceberRequest request);

    Mono<ContaReceber> findById(Long id);

    Mono<List<ContaReceber>> findByFiltro(Long idContrato);

    Mono<Void> criar(Long id);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(ContaReceber entity);

    Mono<Void> deletarContaEAtualizarContrato(Long id);

    Mono<ContaReceberPorMesResumeResponse> fetchResumoByMes(LocalDate dataRef);

}
