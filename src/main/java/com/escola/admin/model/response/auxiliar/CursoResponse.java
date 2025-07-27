package com.escola.admin.model.response.auxiliar;

import com.escola.admin.model.response.EmpresaResponse;

public record CursoResponse(
        Long id,
        String nome,
        String descricao,
        String duracao,
        String categoria,
        Double valorMensalidade,
        Boolean ativo,
        EmpresaResponse empresa
) {
}
