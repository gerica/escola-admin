package com.escola.admin.service;

import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.request.ParametroRequest;
import reactor.core.publisher.Mono;

public interface ParametroService {

    String CHAVE_CONTRATO_CIDADE_PADRAO = "CHAVE_CONTRATO_CIDADE_PADRAO";
    String CHAVE_CONTRATO_MODELO_PADRAO = "CHAVE_CONTRATO_MODELO_PADRAO";
    String CHAVE_CONTRATO_MODELO_PADRAO_MAP = "modeloContrato";

    Mono<Parametro> salvar(ParametroRequest request, Long empresaIdFromToken);

    Mono<Parametro> findByChave(String chave, Long empresaIdFromToken);
}
