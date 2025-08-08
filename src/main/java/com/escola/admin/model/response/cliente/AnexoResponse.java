package com.escola.admin.model.response.cliente;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record AnexoResponse(
        Long id,
        String nomeArquivo,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataCadastro

) {
}