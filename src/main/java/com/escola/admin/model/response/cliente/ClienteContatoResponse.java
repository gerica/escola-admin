package com.escola.admin.model.response.cliente;

import com.escola.admin.model.entity.cliente.TipoContato;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ClienteContatoResponse(
        Long id,
        String numero,
        TipoContato tipoContato,
        String observacao,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataCadastro,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime dataAtualizacao
) {
}
