package com.escola.admin.service.cliente;


import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.request.cliente.ContratoModeloRequest;
import com.escola.admin.model.request.cliente.ContratoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

public interface ContratoService {
    String CHAVE_CONTRATO_MODELO_PADRAO = "CHAVE_CONTRATO_MODELO_PADRAO";

    Mono<Void> save(ContratoRequest request);

    Mono<Void> saveModelo(ContratoModeloRequest request);

    Optional<Page<Contrato>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable);

    Mono<Contrato> findById(Long id);

    Optional<Void> deleteById(Integer id);

    Mono<Contrato> parseContrato(Long idContrato);

    Long count();

    Mono<Matricula> criarContrato(Matricula matricula);

    Mono<Contrato> findByIdMatricula(Long id);

    Mono<Void> deleteByIdMatricula(Long id);

    Mono<BigDecimal> getValorMensalidadePorContratoId(Long idContrato);
}