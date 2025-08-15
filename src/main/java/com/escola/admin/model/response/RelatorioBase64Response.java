package com.escola.admin.model.response;

import lombok.Builder;

@Builder
public record RelatorioBase64Response(
        String nomeArquivo,
        String conteudoBase64
) {
}
