package com.escola.admin.service.cliente;

import com.escola.admin.model.entity.cliente.Contrato;

public interface ArtificalInteligenceService {

    String generateText(String prompt, Contrato contrato);
}
