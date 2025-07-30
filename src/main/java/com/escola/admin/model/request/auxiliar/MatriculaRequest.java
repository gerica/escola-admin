package com.escola.admin.model.request.auxiliar;

import com.escola.admin.model.entity.auxiliar.StatusMatricula;
import lombok.Builder;

@Builder
public record MatriculaRequest(
        Long id,
        Long idTurma,
        Long idCliente,
        Long idClienteDependente,
        StatusMatricula status,
        String observacoes
) {
}
