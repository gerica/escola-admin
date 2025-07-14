package com.escola.admin.service;

import com.escola.admin.model.entity.Parametro;
import com.escola.admin.model.request.ParametroRequest;

import java.util.Optional;

public interface ParametroService {

    String CHAVE_CONTRATO_CIDADE_PADRAO = "CHAVE_CONTRATO_CIDADE_PADRAO";
    String CHAVE_CONTRATO_MODELO_PADRAO = "CHAVE_CONTRATO_MODELO_PADRAO";

    Optional<Parametro> salvar(ParametroRequest request);

    Optional<Parametro> findByChave(String chave);
}
