package com.escola.admin.model.response.auxiliar;

import com.escola.admin.model.response.EmpresaResponse;

public record CursoResponse(
        Long id,
        String nome,
        String descricao,
        Integer duracaoValor,
        String duracaoUnidade,
        String categoria,
        Double valorMensalidade,
        Boolean ativo,
        EmpresaResponse empresa
) {
}
