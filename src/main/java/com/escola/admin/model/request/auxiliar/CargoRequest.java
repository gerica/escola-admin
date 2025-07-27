package com.escola.admin.model.request.auxiliar;

import lombok.Builder;

@Builder
public record CargoRequest(
        Long id,
        Long idEmpresa,
        String nome,
        String descricao,
        Boolean ativo
) {
}
