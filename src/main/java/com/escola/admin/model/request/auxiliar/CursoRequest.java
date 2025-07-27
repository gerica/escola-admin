package com.escola.admin.model.request.auxiliar;

import lombok.Builder;

@Builder
public record CursoRequest(
        Long id,
        Long idEmpresa,
        String nome,
        String descricao,
        String duracao,
        String categoria,
        Double valorMensalidade,
        Boolean ativo
) {
}
