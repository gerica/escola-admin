package com.escola.admin.model.request.cliente;


import com.escola.admin.model.entity.cliente.StatusCliente;

import java.time.LocalDate;

public record ClienteRequest(
        Integer id,
        String nome,
        LocalDate dataNascimento,
        String cidade,
        String uf,
        String codigoCidade,
        String docCPF,
        String docRG,
        String telResidencial,
        String telCelular,
        String endereco,
        String email,
        String profissao,
        String localTrabalho,
        StatusCliente statusCliente
) {
}
