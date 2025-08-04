package com.escola.admin.model.response.cliente;

import lombok.Builder;

@Builder
public record AnexoBase64Response(
        String nomeArquivo,
        String conteudoBase64
) {
}