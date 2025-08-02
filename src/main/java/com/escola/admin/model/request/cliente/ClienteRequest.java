package com.escola.admin.model.request.cliente;


import com.escola.admin.model.entity.cliente.StatusCliente;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ClienteRequest(
        Long id,
        Long idEmpresa,
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
