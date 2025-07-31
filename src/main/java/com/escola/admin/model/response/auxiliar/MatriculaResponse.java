package com.escola.admin.model.response.auxiliar;

import com.escola.admin.model.entity.auxiliar.StatusMatricula;
import com.escola.admin.model.response.cliente.ClienteDependenteResponse;
import com.escola.admin.model.response.cliente.ClienteResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MatriculaResponse(
        Long id,
        String codigo,
        TurmaResponse turma,
        ClienteResponse cliente,
        ClienteDependenteResponse clienteDependente,
        StatusMatricula status,
        String observacoes,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataAtualizacao
) {
}
