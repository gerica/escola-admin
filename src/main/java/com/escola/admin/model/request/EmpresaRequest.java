package com.escola.admin.model.request;

public record EmpresaRequest(
        Long id,
        String nomeFantasia,
        String razaoSocial,
        String cnpj,
        String inscricaoEstadual,
        String telefone,
        String email,
        String endereco,
        String logoBase64,
        String logoMimeType,
        Boolean ativo
) {
}
