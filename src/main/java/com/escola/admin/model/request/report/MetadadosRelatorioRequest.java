package com.escola.admin.model.request.report;

import lombok.Builder;

@Builder
public record MetadadosRelatorioRequest(
        String nomeUsuario,
        String titulo,
        String subtitulo,
        String nomeArquivo,
        String logoBase64
) {
}
