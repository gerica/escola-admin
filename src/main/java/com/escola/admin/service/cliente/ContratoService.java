package com.escola.admin.service.cliente;


import com.escola.admin.model.entity.cliente.Contrato;
import com.escola.admin.model.request.cliente.ContratoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ContratoService {
    String CHAVE_CONTRATO_MODELO_PADRAO = "CHAVE_CONTRATO_MODELO_PADRAO";

    Contrato save(ContratoRequest request);

    Optional<Page<Contrato>> findByFiltro(String filtro, Long idEmpresa, Pageable pageable);

    Optional<Contrato> findById(Long id);

    Optional<Void> deleteById(Integer id);

    Optional<Contrato> parseContrato(Long idContrato);
}
