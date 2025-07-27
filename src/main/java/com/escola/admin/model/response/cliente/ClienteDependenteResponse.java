package com.escola.admin.model.response.cliente;

import com.escola.admin.model.entity.cliente.Sexo;
import com.escola.admin.model.entity.cliente.TipoParentesco;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteDependenteResponse(
        Integer id,
        String nome,
        String docCPF,
        Sexo sexo,
        TipoParentesco parentesco,
        String parentescoDescricao,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataNascimento,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataAtualizacao
) {
}
