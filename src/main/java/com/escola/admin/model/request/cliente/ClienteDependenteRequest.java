package com.escola.admin.model.request.cliente;


import com.escola.admin.model.entity.cliente.Sexo;
import com.escola.admin.model.entity.cliente.TipoParentesco;

import java.time.LocalDate;

public record ClienteDependenteRequest(
        Long id,
        Long idCliente,
        String nome,
        String docCPF,
        Sexo sexo,
        LocalDate dataNascimento,
        TipoParentesco parentesco
) {
}
