package com.escola.admin.model.response.auxiliar;

import com.escola.admin.model.response.EmpresaResponse;

public record CargoResponse(
        Long id,
        String nome,
        String descricao,
        Boolean ativo,
        EmpresaResponse empresa
) {
}
