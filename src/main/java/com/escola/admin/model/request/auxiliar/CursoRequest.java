package com.escola.admin.model.request.auxiliar;

import lombok.Builder;

@Builder
public record CursoRequest(
        Long id,
        Long idEmpresa,
        String nome,
        String descricao,
        Integer duracaoValor,
        String duracaoUnidade,
        String categoria,
        Double valorMensalidade,
        Boolean ativo
) {
}
