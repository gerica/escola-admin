package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.ContaReceber;
import com.escola.admin.model.request.cliente.ContaReceberRequest;
import com.escola.admin.model.response.cliente.ContaReceberPorMesDetalheResponse;
import com.escola.admin.model.response.cliente.ContaReceberPorMesResumeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaReceberService {

    Mono<ContaReceber> save(ContaReceberRequest request);

    Mono<ContaReceber> findById(Long id);

    Mono<List<ContaReceber>> findByFiltro(Long idContrato);

    Mono<Void> criar(Long id);

    Mono<Void> deleteById(Long id);

    Mono<Void> delete(ContaReceber entity);

    Mono<Void> deletarContaEAtualizarContrato(Long id);

    Mono<ContaReceberPorMesResumeResponse> fetchResumoByMes(LocalDate dataRef);

    Optional<Page<ContaReceberPorMesDetalheResponse>> findByDataRef(LocalDate dataRef, Pageable pageable);
}
