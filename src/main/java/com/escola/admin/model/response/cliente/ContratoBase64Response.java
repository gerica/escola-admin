package com.escola.admin.model.response.cliente;

import lombok.Builder;

@Builder
public record ContratoBase64Response(
        String nomeArquivo,
        String conteudoBase64
) {
}