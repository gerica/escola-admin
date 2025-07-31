package com.escola.admin.service.cliente;


import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.request.cliente.ContratoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface ContratoService {
    String CHAVE_CONTRATO_MODELO_PADRAO = "CHAVE_CONTRATO_MODELO_PADRAO";

    Contrato save(ContratoRequest request);

    Optional<Page<Contrato>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable);

    Mono<Contrato> findById(Long id);

    Optional<Void> deleteById(Integer id);

    Mono<Contrato> parseContrato(Long idContrato);
}
