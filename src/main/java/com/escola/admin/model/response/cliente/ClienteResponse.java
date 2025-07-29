package com.escola.admin.model.response.cliente;

import com.escola.admin.model.entity.cliente.StatusCliente;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClienteResponse(
        Long id,
        String nome,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataNascimento,
        String cidadeDesc,
        String uf,
        String codigoCidade,
        String docCPF,
        String docRG,
        String endereco,
        String email,
        String profissao,
        String localTrabalho,
        StatusCliente statusCliente,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataAtualizacao,
        List<ClienteDependenteResponse> dependentes

) {
}
