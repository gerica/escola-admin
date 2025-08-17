package com.escola.admin.service.cliente;


import com.escola.admin.model.entity.Usuario;
import com.escola.admin.model.entity.auxiliar.Matricula;
import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.entity.cliente.StatusContrato;
import com.escola.admin.model.request.cliente.ContratoModeloRequest;
import com.escola.admin.model.request.cliente.ContratoRequest;
import com.escola.admin.model.request.report.FiltroRelatorioRequest;
import com.escola.admin.model.response.RelatorioBase64Response;
import com.escola.admin.model.response.cliente.ContratoBase64Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContratoService {
    String CHAVE_CONTRATO_MODELO_PADRAO = "CHAVE_CONTRATO_MODELO_PADRAO";

    Mono<Void> save(ContratoRequest request);

    Mono<Void> saveModelo(ContratoModeloRequest request);

    Optional<Page<Contrato>> findByFiltro(String filtro, Long idEmpresa, List<StatusContrato> statusContrato, Pageable pageable);

    Mono<Contrato> findById(Long id);

    Optional<Void> deleteById(Integer id);

    Mono<Contrato> parseContrato(Long idContrato, Long empresaIdFromToken);

    Long count();

    Mono<Matricula> criarContrato(Matricula matricula, Long empresaIdFromToken);

    Mono<Contrato> findByIdMatricula(Long id);

    Mono<Void> deleteByIdMatricula(Long id);

    Mono<BigDecimal> getValorMensalidadePorContratoId(Long idContrato);

    Mono<ContratoBase64Response> downloadDocContrato(Long id);

    Mono<RelatorioBase64Response> emitirRelatorio(FiltroRelatorioRequest request, Usuario empresaIdFromToken);
}